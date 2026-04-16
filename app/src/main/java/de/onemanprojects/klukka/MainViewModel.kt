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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
                    val event = TrackingStartedEvent(tracked.id, project, startMillis)
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

    /**
     * Parses the server's start-time string (e.g. "Apr 16, 2026, 5:34:29 AM") as UTC.
     * The server always returns times in UTC; subtracting this from System.currentTimeMillis()
     * (also UTC) gives the correct elapsed duration.
     */
    private fun parseStartTime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(raw)?.time
        } catch (e: Exception) {
            AppLogger.w(TAG, "Could not parse start time '$raw': ${e.message}")
            null
        }
    }

    /** Called by ActiveTrackingFragment when the session stops. */
    fun onTrackingStopped() {
        AppLogger.d(TAG, "Tracking stopped")
        _activeTracking.value = null
    }
}
