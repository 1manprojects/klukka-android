package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.AnalysisData
import de.onemanprojects.klukka.model.DataFilter
import de.onemanprojects.klukka.model.UpdateTrackedRequest
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "CalendarViewModel"

enum class CalendarViewType { DAY, WEEK, WORK_WEEK }

data class CalendarState(
    val days: List<LocalDate>,
    val title: String
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private var viewType = CalendarViewType.WEEK
    private var referenceDate = LocalDate.now()

    private val _state = MutableLiveData<CalendarState>()
    val state: LiveData<CalendarState> = _state

    private val _data = MutableLiveData<AnalysisData?>()
    val data: LiveData<AnalysisData?> = _data

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // null = idle, true = success, false = error
    private val _trackedUpdated = MutableLiveData<Boolean?>(null)
    val trackedUpdated: LiveData<Boolean?> = _trackedUpdated

    private val _trackedDeleted = MutableLiveData<Boolean?>(null)
    val trackedDeleted: LiveData<Boolean?> = _trackedDeleted

    init {
        updateState()
    }

    fun setViewType(type: CalendarViewType) {
        if (type == viewType) return
        viewType = type
        updateState()
    }

    fun navigatePrev() {
        referenceDate = referenceDate.minus(stepPeriod())
        updateState()
    }

    fun navigateNext() {
        referenceDate = referenceDate.plus(stepPeriod())
        updateState()
    }

    fun navigateToday() {
        referenceDate = LocalDate.now()
        updateState()
    }

    private fun stepPeriod(): Period = when (viewType) {
        CalendarViewType.DAY -> Period.ofDays(1)
        CalendarViewType.WEEK, CalendarViewType.WORK_WEEK -> Period.ofWeeks(1)
    }

    private fun updateState() {
        val (start, end, days) = computeRange()
        _state.value = CalendarState(days, formatTitle(start, end))
        loadData(start, end)
    }

    private fun computeRange(): Triple<LocalDate, LocalDate, List<LocalDate>> = when (viewType) {
        CalendarViewType.DAY ->
            Triple(referenceDate, referenceDate, listOf(referenceDate))
        CalendarViewType.WEEK -> {
            val mon = referenceDate.with(DayOfWeek.MONDAY)
            Triple(mon, mon.plusDays(6), (0L..6L).map { mon.plusDays(it) })
        }
        CalendarViewType.WORK_WEEK -> {
            val mon = referenceDate.with(DayOfWeek.MONDAY)
            Triple(mon, mon.plusDays(4), (0L..4L).map { mon.plusDays(it) })
        }
    }

    private fun formatTitle(start: LocalDate, end: LocalDate): String {
        val shortFmt = DateTimeFormatter.ofPattern("d MMM")
        val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
        val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")
        return when {
            start == end -> start.format(dayFmt)
            start.year == end.year -> "${start.format(shortFmt)} – ${end.format(fullFmt)}"
            else -> "${start.format(fullFmt)} – ${end.format(fullFmt)}"
        }
    }

    fun updateTracked(request: UpdateTrackedRequest) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.updateTracked("Bearer $apiToken", request)
                AppLogger.i(TAG, "Updated tracked entry id=${request.id}")
                updateState()
                _trackedUpdated.value = true
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error updating tracked entry: ${e.code()}", e)
                _error.value = "Failed to save changes (${e.code()})"
                _trackedUpdated.value = false
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error updating tracked entry", e)
                _error.value = "Network error: could not reach the server"
                _trackedUpdated.value = false
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error updating tracked entry", e)
                _error.value = "Failed to save changes"
                _trackedUpdated.value = false
            } finally {
                _loading.value = false
            }
        }
    }

    fun onUpdateHandled() {
        _trackedUpdated.value = null
    }

    fun deleteTracked(id: Int) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.deleteTracked("Bearer $apiToken", id)
                AppLogger.i(TAG, "Deleted tracked entry id=$id")
                updateState()
                _trackedDeleted.value = true
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error deleting tracked entry: ${e.code()}", e)
                _error.value = "Failed to delete entry (${e.code()})"
                _trackedDeleted.value = false
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error deleting tracked entry", e)
                _error.value = "Network error: could not reach the server"
                _trackedDeleted.value = false
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error deleting tracked entry", e)
                _error.value = "Failed to delete entry"
                _trackedDeleted.value = false
            } finally {
                _loading.value = false
            }
        }
    }

    fun onDeleteHandled() {
        _trackedDeleted.value = null
    }

    private fun loadData(start: LocalDate, end: LocalDate) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val startStr = start.atStartOfDay().atOffset(ZoneOffset.UTC).format(fmt)
                val endStr = end.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).format(fmt)
                val response = service.getData(
                    "Bearer $apiToken",
                    DataFilter(startStr, endStr, null)
                )
                AppLogger.i(TAG, "Loaded calendar data: ${response.payload?.tracked?.size ?: 0} entries")
                _data.value = response.payload
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error loading calendar data: ${e.code()}", e)
                _error.value = "Failed to load calendar data (${e.code()})"
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error loading calendar data", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading calendar data", e)
                _error.value = "Failed to load calendar data"
            } finally {
                _loading.value = false
            }
        }
    }
}
