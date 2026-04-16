package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.StartRequest
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.TimeZone

private const val TAG = "ProjectsViewModel"

data class TrackingStartedEvent(val trackingId: Int, val project: Project, val startTime: Long)

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _unauthorized = MutableLiveData<Boolean>()
    val unauthorized: LiveData<Boolean> = _unauthorized

    private val _trackingStarted = MutableLiveData<TrackingStartedEvent?>()
    val trackingStarted: LiveData<TrackingStartedEvent?> = _trackingStarted

    fun loadProjects() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        AppLogger.i(TAG, "Loading projects from $serverUrl")
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val result = service.getProjects("Bearer $apiToken")
                val allProjects = (result.own ?: emptyList()) + (result.group ?: emptyList())
                AppLogger.i(TAG, "Loaded ${allProjects.size} projects (own=${result.own?.size ?: 0}, group=${result.group?.size ?: 0})")
                _projects.value = allProjects
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error loading projects: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to load projects — server error (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error loading projects", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading projects", e)
                _error.value = "Failed to load projects"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startTracking(project: Project) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        AppLogger.i(TAG, "Starting tracking for project id=${project.id} title=${project.title}")

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val response = service.startTracking(
                    "Bearer $apiToken",
                    StartRequest(project.id, TimeZone.getDefault().id)
                )
                val trackingId = response.payload?.asInt
                    ?: throw Exception("Invalid tracking ID in response")
                AppLogger.i(TAG, "Tracking started, id=$trackingId")
                _trackingStarted.value = TrackingStartedEvent(trackingId, project, System.currentTimeMillis())
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error starting tracking: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to start tracking — server error (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error starting tracking", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting tracking", e)
                _error.value = "Failed to start tracking"
            }
        }
    }

    fun onTrackingNavigated() {
        _trackingStarted.value = null
    }
}
