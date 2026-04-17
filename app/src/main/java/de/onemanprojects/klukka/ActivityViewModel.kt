package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.AnalysisData
import de.onemanprojects.klukka.model.DataFilter
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.Tracked
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "ActivityViewModel"

enum class ActivityPreset { TODAY, WEEK, MONTH, CUSTOM }

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _preset = MutableLiveData(ActivityPreset.WEEK)
    val preset: LiveData<ActivityPreset> = _preset

    private val _startDate = MutableLiveData<LocalDate>()
    val startDate: LiveData<LocalDate> = _startDate

    private val _endDate = MutableLiveData<LocalDate>()
    val endDate: LiveData<LocalDate> = _endDate

    private val _barData = MutableLiveData<List<Pair<LocalDate, Long>>>(emptyList())
    val barData: LiveData<List<Pair<LocalDate, Long>>> = _barData

    private val _projectTotals = MutableLiveData<List<Pair<Project, Long>>>(emptyList())
    val projectTotals: LiveData<List<Pair<Project, Long>>> = _projectTotals

    private val _totalMinutes = MutableLiveData(0L)
    val totalMinutes: LiveData<Long> = _totalMinutes

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _unauthorized = MutableLiveData<Boolean>()
    val unauthorized: LiveData<Boolean> = _unauthorized

    init {
        selectPreset(ActivityPreset.WEEK)
    }

    fun selectPreset(preset: ActivityPreset) {
        _preset.value = preset
        val today = LocalDate.now()
        when (preset) {
            ActivityPreset.TODAY -> {
                _startDate.value = today
                _endDate.value = today
            }
            ActivityPreset.WEEK -> {
                _startDate.value = today.with(DayOfWeek.MONDAY)
                _endDate.value = today.with(DayOfWeek.SUNDAY)
            }
            ActivityPreset.MONTH -> {
                _startDate.value = today.withDayOfMonth(1)
                _endDate.value = today
            }
            ActivityPreset.CUSTOM -> return
        }
        loadData()
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _preset.value = ActivityPreset.CUSTOM
        _startDate.value = start
        _endDate.value = end
        loadData()
    }

    fun loadData() {
        val start = _startDate.value ?: return
        val end = _endDate.value ?: return
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Loading activity data $start – $end")
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val startStr = start.atStartOfDay().atOffset(ZoneOffset.UTC).format(fmt)
                val endStr = end.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).format(fmt)
                val response = service.getData("Bearer $apiToken", DataFilter(startStr, endStr, null))
                AppLogger.i(TAG, "Loaded activity data: ${response.payload?.tracked?.size ?: 0} entries")
                processData(response.payload, start, end)
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error loading activity: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to load activity data (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error loading activity", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading activity", e)
                _error.value = "Failed to load activity data"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun processData(data: AnalysisData?, start: LocalDate, end: LocalDate) {
        if (data == null) {
            _barData.value = buildEmptyBarData(start, end)
            _projectTotals.value = emptyList()
            _totalMinutes.value = 0L
            return
        }

        val tracked = data.tracked ?: emptyList()
        val allProjects = (data.projects ?: emptyList()) + (data.groupProjects ?: emptyList())
        val projectsById = allProjects.associateBy { it.id }
        val zoneId = ZoneId.systemDefault()

        val minutesByDate = mutableMapOf<LocalDate, Long>()
        var d = start
        while (!d.isAfter(end)) {
            minutesByDate[d] = 0L
            d = d.plusDays(1)
        }

        val minutesByProject = mutableMapOf<Int, Long>()
        var totalMins = 0L

        for (t in tracked) {
            val startMs = Tracked.parseToEpochMillis(t.start) ?: continue
            val endMs = Tracked.parseToEpochMillis(t.end) ?: continue
            val durationMins = ((endMs - startMs) / 60_000L).coerceAtLeast(0L)
            val date = Instant.ofEpochMilli(startMs).atZone(zoneId).toLocalDate()
            minutesByDate[date] = (minutesByDate[date] ?: 0L) + durationMins
            minutesByProject[t.projectId] = (minutesByProject[t.projectId] ?: 0L) + durationMins
            totalMins += durationMins
        }

        _barData.value = minutesByDate.entries.sortedBy { it.key }.map { Pair(it.key, it.value) }
        _totalMinutes.value = totalMins
        _projectTotals.value = minutesByProject.entries
            .sortedByDescending { it.value }
            .mapNotNull { (projectId, mins) ->
                val project = projectsById[projectId] ?: return@mapNotNull null
                Pair(project, mins)
            }
    }

    private fun buildEmptyBarData(start: LocalDate, end: LocalDate): List<Pair<LocalDate, Long>> {
        val result = mutableListOf<Pair<LocalDate, Long>>()
        var d = start
        while (!d.isAfter(end)) {
            result.add(Pair(d, 0L))
            d = d.plusDays(1)
        }
        return result
    }
}
