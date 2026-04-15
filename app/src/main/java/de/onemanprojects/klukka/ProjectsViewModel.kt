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
import java.util.TimeZone

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

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val result = service.getProjects("Bearer $apiToken")
                val allProjects = (result.own ?: emptyList()) + (result.group ?: emptyList())
                _projects.value = allProjects
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = e.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startTracking(project: Project) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val response = service.startTracking(
                    "Bearer $apiToken",
                    StartRequest(project.id, TimeZone.getDefault().id)
                )
                val trackingId = response.payload?.asInt
                    ?: throw Exception("Invalid tracking ID in response")
                _trackingStarted.value = TrackingStartedEvent(trackingId, project, System.currentTimeMillis())
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = e.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun onTrackingNavigated() {
        _trackingStarted.value = null
    }
}
