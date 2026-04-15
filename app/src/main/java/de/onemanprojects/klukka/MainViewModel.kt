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

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val tracked = service.getActiveTracking("Bearer $apiToken")
                if (tracked.active) {
                    val userProjects = service.getProjects("Bearer $apiToken")
                    val allProjects = (userProjects.own ?: emptyList()) + (userProjects.group ?: emptyList())
                    val project = allProjects.find { it.id == tracked.projectId }
                        ?: Project(tracked.projectId, null, null, null, 0.0, tracked.projectId, false)
                    val startMillis = tracked.start?.time ?: System.currentTimeMillis()
                    val event = TrackingStartedEvent(tracked.id, project, startMillis)
                    _activeTracking.value = event
                    _pendingNavToTracking.value = event
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                }
                // Other codes: no active tracking – silently ignore
            } catch (e: Exception) {
                // Network error on startup – silently ignore
            }
        }
    }

    /** Called by ProjectsFragment when a tracking session starts. */
    fun onTrackingStarted(event: TrackingStartedEvent) {
        _activeTracking.value = event
        _pendingNavToTracking.value = event
    }

    /** Called by MainActivity after it has handled the navigation event. */
    fun onNavigatedToTracking() {
        _pendingNavToTracking.value = null
    }

    /** Called by ActiveTrackingFragment when the session stops. */
    fun onTrackingStopped() {
        _activeTracking.value = null
    }
}
