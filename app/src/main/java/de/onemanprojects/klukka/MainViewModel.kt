package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonPrimitive
import de.onemanprojects.klukka.model.ApiResponse
import de.onemanprojects.klukka.model.CommentUpdate
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.StartRequest
import de.onemanprojects.klukka.model.Tracked
import de.onemanprojects.klukka.model.UpdateTrackedRequest
import de.onemanprojects.klukka.network.ApiClient
import de.onemanprojects.klukka.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone

private const val TAG = "MainViewModel"
private val SERVER_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
private const val SYNC_RETRY_MS = 30_000L

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)
    private val offlineCache = OfflineCache(application)
    private val connectivityMonitor = ConnectivityMonitor(application)

    // Prevents concurrent sync runs (connectivity callbacks fire multiple times on reconnect)
    private val syncMutex = Mutex()

    val isOnline: LiveData<Boolean> = connectivityMonitor.isOnline

    private val _activeTracking = MutableLiveData<TrackingStartedEvent?>(null)
    val activeTracking: LiveData<TrackingStartedEvent?> = _activeTracking

    private val _pendingNavToTracking = MutableLiveData<TrackingStartedEvent?>(null)
    val pendingNavToTracking: LiveData<TrackingStartedEvent?> = _pendingNavToTracking

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    init {
        // Attempt sync every time connectivity reports online. This covers:
        //   - clean offline → online transitions (wifi toggle)
        //   - initial startup when there are actions from a previous session
        // The mutex ensures only one sync runs at a time even if the callback fires repeatedly.
        connectivityMonitor.isOnline.observeForever { online ->
            if (online) {
                viewModelScope.launch { syncPendingActions() }
            }
        }

        // Periodic fallback: if the server was temporarily unreachable while wifi stayed
        // connected there is no network state-change event, so the observer above never fires.
        // Every 30 seconds we retry so the queue drains without user interaction.
        viewModelScope.launch {
            while (true) {
                delay(SYNC_RETRY_MS)
                if (connectivityMonitor.checkNow()) {
                    syncPendingActions()
                }
            }
        }
    }

    fun checkActiveTracking() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        if (serverUrl.isEmpty() || apiToken.isEmpty()) return

        AppLogger.d(TAG, "Checking for active tracking session")
        viewModelScope.launch {
            if (!connectivityMonitor.checkNow()) {
                // Offline: restore an active offline session from cache if one exists
                val activeStart = offlineCache.getActiveOfflineStart()
                if (activeStart != null) {
                    AppLogger.i(TAG, "Offline: restoring offline tracking session from cache")
                    val event = TrackingStartedEvent(-1, activeStart.project, activeStart.startTimeMs, activeStart.comment)
                    _activeTracking.postValue(event)
                    _pendingNavToTracking.postValue(event)
                }
                return@launch
            }

            // Wait for any in-progress sync to finish before reading server state.
            // This matters for OnlineStop: the server still shows the session as active
            // until the stop request is sent, so we must sync first.
            syncMutex.withLock { }

            try {
                val service = ApiClient.create(serverUrl)
                val trackedResponse = service.getActiveTracking("Bearer $apiToken")
                val tracked = trackedResponse.payload
                if (tracked != null && tracked.active) {
                    AppLogger.i(TAG, "Active tracking found: id=${tracked.id} projectId=${tracked.projectId}")
                    val userProjects = service.getProjects("Bearer $apiToken")
                    val allProjects = (userProjects.payload?.own ?: emptyList()) + (userProjects.payload?.group ?: emptyList())
                    val project = allProjects.find { it.id == tracked.projectId }
                        ?: Project(tracked.projectId, null, null, null, 0.0, tracked.projectId, false)
                    val startMillis = Tracked.parseToEpochMillis(tracked.start) ?: System.currentTimeMillis()
                    AppLogger.i(TAG, "Active tracking start epoch: $startMillis elapsed=${(System.currentTimeMillis() - startMillis) / 1000}s")
                    val event = TrackingStartedEvent(tracked.id, project, startMillis, tracked.comment ?: "")
                    _activeTracking.postValue(event)
                    _pendingNavToTracking.postValue(event)
                } else {
                    AppLogger.i(TAG, "No active tracking session")
                }
            } catch (e: HttpException) {
                AppLogger.w(TAG, "HTTP ${e.code()} checking active tracking")
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.postValue(true)
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not check active tracking: ${e.message}")
            }
        }
    }

    private suspend fun syncPendingActions() {
        syncMutex.withLock {
            val actions = offlineCache.getPendingActions()
            if (actions.isEmpty()) return@withLock
            AppLogger.i(TAG, "Syncing ${actions.size} pending action(s)")
            for (action in actions) {
                try {
                    doSyncAction(action)
                    offlineCache.removeFirstPendingAction()
                    AppLogger.i(TAG, "Synced and removed: ${action::class.simpleName}")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Sync failed for ${action::class.simpleName}: ${e.message}", e)
                    break // Retry on the next connectivity event or periodic tick
                }
            }
        }
    }

    private suspend fun doSyncAction(action: PendingTrackingAction) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        if (serverUrl.isEmpty() || apiToken.isEmpty()) return
        val service = ApiClient.create(serverUrl)
        when (action) {
            is PendingTrackingAction.OfflineStart -> syncOfflineStart(service, apiToken, action)
            is PendingTrackingAction.OnlineStop -> syncOnlineStop(service, apiToken, action)
            is PendingTrackingAction.OfflineStartAndStop -> syncOfflineStartAndStop(service, apiToken, action)
        }
    }

    private suspend fun syncOfflineStart(service: ApiService, apiToken: String, action: PendingTrackingAction.OfflineStart) {
        val resp = service.startTracking("Bearer $apiToken", StartRequest(action.project.id, action.timezone))
        val realId = extractId(service, apiToken, resp) ?: throw Exception("Could not get tracking ID from server")
        service.updateTracked("Bearer $apiToken", UpdateTrackedRequest(
            id = realId,
            projectId = action.project.id,
            active = true,
            start = epochToIso(action.startTimeMs),
            end = null,
            timezone = action.timezone,
            comment = action.comment.ifEmpty { null }
        ))
        val updatedEvent = TrackingStartedEvent(realId, action.project, action.startTimeMs, action.comment)
        _activeTracking.postValue(updatedEvent)
        AppLogger.i(TAG, "Synced offline start → real ID=$realId")
    }

    private suspend fun syncOnlineStop(service: ApiService, apiToken: String, action: PendingTrackingAction.OnlineStop) {
        if (action.comment.isNotEmpty()) {
            runCatching { service.updateComment("Bearer $apiToken", CommentUpdate(action.trackingId, action.comment)) }
        }
        service.stopTracking("Bearer $apiToken", JsonPrimitive(action.trackingId))
        runCatching {
            service.updateTracked("Bearer $apiToken", UpdateTrackedRequest(
                id = action.trackingId,
                projectId = action.projectId,
                active = false,
                start = null,
                end = epochToIso(action.endTimeMs),
                timezone = TimeZone.getDefault().id,
                comment = action.comment.ifEmpty { null }
            ))
        }
        AppLogger.i(TAG, "Synced online stop for tracking id=${action.trackingId}")
    }

    private suspend fun syncOfflineStartAndStop(service: ApiService, apiToken: String, action: PendingTrackingAction.OfflineStartAndStop) {
        val resp = service.startTracking("Bearer $apiToken", StartRequest(action.project.id, action.timezone))
        val realId = extractId(service, apiToken, resp) ?: throw Exception("Could not get tracking ID from server")
        service.stopTracking("Bearer $apiToken", JsonPrimitive(realId))
        runCatching {
            service.updateTracked("Bearer $apiToken", UpdateTrackedRequest(
                id = realId,
                projectId = action.project.id,
                active = false,
                start = epochToIso(action.startTimeMs),
                end = epochToIso(action.endTimeMs),
                timezone = action.timezone,
                comment = action.comment.ifEmpty { null }
            ))
        }
        AppLogger.i(TAG, "Synced offline start+stop as tracking id=$realId")
    }

    private suspend fun extractId(service: ApiService, apiToken: String, resp: ApiResponse): Int? {
        val payload = resp.payload ?: return null
        return when {
            payload.isJsonPrimitive && payload.asJsonPrimitive.isNumber -> payload.asInt
            payload.isJsonPrimitive && payload.asJsonPrimitive.isBoolean && payload.asBoolean ->
                service.getActiveTracking("Bearer $apiToken").payload?.id
            else -> null
        }
    }

    private fun epochToIso(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atOffset(ZoneOffset.UTC).format(SERVER_FMT)

    fun onTrackingStarted(event: TrackingStartedEvent) {
        AppLogger.d(TAG, "Tracking started: id=${event.trackingId} project=${event.project.title}")
        _activeTracking.value = event
        _pendingNavToTracking.value = event
    }

    fun onNavigatedToTracking() {
        _pendingNavToTracking.value = null
    }

    fun onTrackingStopped() {
        AppLogger.d(TAG, "Tracking stopped")
        _activeTracking.value = null
    }

    override fun onCleared() {
        connectivityMonitor.unregister()
        super.onCleared()
    }
}
