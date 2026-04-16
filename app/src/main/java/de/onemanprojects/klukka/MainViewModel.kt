package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    // Holds tracking data while tracking is active; null means not tracking
    private val _activeTracking = MutableLiveData<TrackingStartedEvent?>(null)
    val activeTracking: LiveData<TrackingStartedEvent?> = _activeTracking

    // One-shot navigation event consumed by MainActivity after navigating
    private val _pendingNavToTracking = MutableLiveData<TrackingStartedEvent?>(null)
    val pendingNavToTracking: LiveData<TrackingStartedEvent?> = _pendingNavToTracking

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    /** Called on app start to restore active tracking if one exists on the server. */
    fun checkActiveTracking() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        if (serverUrl.isEmpty() || apiToken.isEmpty()) return

        AppLogger.d(TAG, "Checking for active tracking session")
        viewModelScope.launch {
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
                    val startMillis = parseStartTime(tracked.start) ?: System.currentTimeMillis()
                    AppLogger.i(TAG, "Active tracking start epoch: $startMillis elapsed=${(System.currentTimeMillis() - startMillis) / 1000}s")
                    val event = TrackingStartedEvent(tracked.id, project, startMillis, tracked.comment ?: "")
                    _activeTracking.value = event
                    _pendingNavToTracking.value = event
                } else {
                    AppLogger.i(TAG, "No active tracking session")
                }
            } catch (e: HttpException) {
                AppLogger.w(TAG, "HTTP ${e.code()} checking active tracking")
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                }
                // Other codes: no active tracking – silently ignore
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not check active tracking: ${e.message}")
                // Network error on startup – silently ignore
            }
        }
    }

    /** Called by ProjectsFragment when a tracking session starts. */
    fun onTrackingStarted(event: TrackingStartedEvent) {
        AppLogger.d(TAG, "Tracking started: id=${event.trackingId} project=${event.project.title}")
        _activeTracking.value = event
        _pendingNavToTracking.value = event
    }

    /** Called by MainActivity after it has handled the navigation event. */
    fun onNavigatedToTracking() {
        _pendingNavToTracking.value = null
    }

    // Matches "Apr 16, 2026, 5:34:29 AM" — uses \s to handle regular and narrow no-break spaces
    // Server always sends UTC regardless of the timezone field
    private val START_TIME_REGEX = Regex(
        """(\w{3})\s+(\d{1,2}),\s+(\d{4}),\s+(\d{1,2}):(\d{2}):(\d{2})\s+(AM|PM)""",
        RegexOption.IGNORE_CASE
    )
    private val MONTH_MAP = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
        "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    private fun parseStartTime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val match = START_TIME_REGEX.find(raw.trim())
        if (match == null) {
            AppLogger.w(TAG, "Could not parse start time '$raw': no regex match")
            return null
        }
        val (monthStr, dayStr, yearStr, hourStr, minStr, secStr, ampm) = match.destructured
        val month = MONTH_MAP[monthStr.lowercase()] ?: run {
            AppLogger.w(TAG, "Unknown month '$monthStr' in '$raw'")
            return null
        }
        var hour = hourStr.toInt()
        if (ampm.uppercase() == "PM" && hour != 12) hour += 12
        if (ampm.uppercase() == "AM" && hour == 12) hour = 0
        val ldt = LocalDateTime.of(yearStr.toInt(), month, dayStr.toInt(), hour, minStr.toInt(), secStr.toInt())
        val millis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli()
        AppLogger.i(TAG, "Parsed start time '$raw' → ${millis}ms")
        return millis
    }

    /** Called by ActiveTrackingFragment when the session stops. */
    fun onTrackingStopped() {
        AppLogger.d(TAG, "Tracking stopped")
        _activeTracking.value = null
    }
}
