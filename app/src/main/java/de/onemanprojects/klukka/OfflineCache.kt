package de.onemanprojects.klukka

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.ProjectSections

sealed class PendingTrackingAction {
    data class OfflineStart(
        val project: Project,
        val startTimeMs: Long,
        val timezone: String,
        val comment: String
    ) : PendingTrackingAction()

    data class OnlineStop(
        val trackingId: Int,
        val projectId: Int,
        val endTimeMs: Long,
        val comment: String
    ) : PendingTrackingAction()

    data class OfflineStartAndStop(
        val project: Project,
        val startTimeMs: Long,
        val endTimeMs: Long,
        val timezone: String,
        val comment: String
    ) : PendingTrackingAction()
}

private data class PendingActionDto(
    val type: String,
    val project: Project? = null,
    val trackingId: Int? = null,
    val projectId: Int? = null,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val timezone: String? = null,
    val comment: String? = null
)

private fun PendingTrackingAction.toDto(): PendingActionDto = when (this) {
    is PendingTrackingAction.OfflineStart ->
        PendingActionDto("OFFLINE_START", project = project, startTimeMs = startTimeMs, timezone = timezone, comment = comment)
    is PendingTrackingAction.OnlineStop ->
        PendingActionDto("ONLINE_STOP", trackingId = trackingId, projectId = projectId, endTimeMs = endTimeMs, comment = comment)
    is PendingTrackingAction.OfflineStartAndStop ->
        PendingActionDto("OFFLINE_START_STOP", project = project, startTimeMs = startTimeMs, endTimeMs = endTimeMs, timezone = timezone, comment = comment)
}

private fun PendingActionDto.toAction(): PendingTrackingAction? {
    return when (type) {
        "OFFLINE_START" -> {
            val p = project ?: return null
            val s = startTimeMs ?: return null
            val tz = timezone ?: return null
            PendingTrackingAction.OfflineStart(p, s, tz, comment ?: "")
        }
        "ONLINE_STOP" -> {
            val tid = trackingId ?: return null
            val pid = projectId ?: return null
            val e = endTimeMs ?: return null
            PendingTrackingAction.OnlineStop(tid, pid, e, comment ?: "")
        }
        "OFFLINE_START_STOP" -> {
            val p = project ?: return null
            val s = startTimeMs ?: return null
            val e = endTimeMs ?: return null
            val tz = timezone ?: return null
            PendingTrackingAction.OfflineStartAndStop(p, s, e, tz, comment ?: "")
        }
        else -> null
    }
}

class OfflineCache(context: Context) {

    private val prefs = context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val projectListType = object : TypeToken<List<Project>>() {}.type
    private val dtoListType = object : TypeToken<MutableList<PendingActionDto>>() {}.type

    fun cacheProjects(sections: ProjectSections) {
        prefs.edit()
            .putString("own_projects", gson.toJson(sections.own, projectListType))
            .putString("group_projects", gson.toJson(sections.group, projectListType))
            .apply()
    }

    fun getCachedProjects(): ProjectSections? {
        val ownJson = prefs.getString("own_projects", null) ?: return null
        val groupJson = prefs.getString("group_projects", null) ?: return null
        val own: List<Project> = gson.fromJson(ownJson, projectListType)
        val group: List<Project> = gson.fromJson(groupJson, projectListType)
        return ProjectSections(own, group)
    }

    fun addPendingAction(action: PendingTrackingAction) {
        val dtos = getDtos()
        dtos.add(action.toDto())
        saveDtos(dtos)
    }

    fun getPendingActions(): List<PendingTrackingAction> =
        getDtos().mapNotNull { it.toAction() }

    fun removeFirstPendingAction() {
        val dtos = getDtos()
        if (dtos.isNotEmpty()) {
            dtos.removeAt(0)
            saveDtos(dtos)
        }
    }

    fun clearAllPendingActions() {
        prefs.edit().remove("pending_actions").apply()
    }

    fun getActiveOfflineStart(): PendingTrackingAction.OfflineStart? =
        getPendingActions().filterIsInstance<PendingTrackingAction.OfflineStart>().lastOrNull()

    fun updatePendingComment(comment: String) {
        val dtos = getDtos()
        val idx = dtos.indexOfLast { it.type == "OFFLINE_START" }
        if (idx != -1) {
            dtos[idx] = dtos[idx].copy(comment = comment)
            saveDtos(dtos)
        }
    }

    fun convertOfflineStartToStartStop(endTimeMs: Long, comment: String) {
        val dtos = getDtos()
        val idx = dtos.indexOfLast { it.type == "OFFLINE_START" }
        if (idx != -1) {
            val dto = dtos[idx]
            dtos[idx] = dto.copy(type = "OFFLINE_START_STOP", endTimeMs = endTimeMs, comment = comment)
            saveDtos(dtos)
        }
    }

    private fun getDtos(): MutableList<PendingActionDto> {
        val json = prefs.getString("pending_actions", null) ?: return mutableListOf()
        return try {
            gson.fromJson(json, dtoListType) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveDtos(dtos: List<PendingActionDto>) {
        prefs.edit().putString("pending_actions", gson.toJson(dtos)).apply()
    }
}
