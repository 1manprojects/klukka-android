package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.ArchiveRequest
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.ProjectSections
import de.onemanprojects.klukka.model.StartRequest
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.TimeZone

private const val TAG = "ProjectsViewModel"

data class TrackingStartedEvent(val trackingId: Int, val project: Project, val startTime: Long, val comment: String = "")

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _projects = MutableLiveData<ProjectSections>()
    val projects: LiveData<ProjectSections> = _projects

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _unauthorized = MutableLiveData<Boolean>()
    val unauthorized: LiveData<Boolean> = _unauthorized

    private val _trackingStarted = MutableLiveData<TrackingStartedEvent?>()
    val trackingStarted: LiveData<TrackingStartedEvent?> = _trackingStarted

    // null = idle, true = success, false = error
    private val _projectCreated = MutableLiveData<Boolean?>(null)
    val projectCreated: LiveData<Boolean?> = _projectCreated

    private val _projectArchived = MutableLiveData<Boolean?>(null)
    val projectArchived: LiveData<Boolean?> = _projectArchived

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
                val own = result.payload?.own ?: emptyList()
                val group = result.payload?.group ?: emptyList()
                AppLogger.i(TAG, "Loaded projects (own=${own.size}, group=${group.size})")
                _projects.value = ProjectSections(own, group)
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

    fun startTracking(project: Project, currentTrackingId: Int? = null) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        AppLogger.i(TAG, "Starting tracking for project id=${project.id} title=${project.title}" +
                (if (currentTrackingId != null) " (stopping current id=$currentTrackingId first)" else ""))

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)

                if (currentTrackingId != null) {
                    AppLogger.i(TAG, "Stopping current tracking id=$currentTrackingId")
                    service.stopTracking("Bearer $apiToken", currentTrackingId)
                    AppLogger.i(TAG, "Current tracking stopped")
                }

                val response = service.startTracking(
                    "Bearer $apiToken",
                    StartRequest(project.id, TimeZone.getDefault().id)
                )
                val trackingId = response.payload?.asInt
                    ?: throw Exception("Invalid tracking ID in response")
                AppLogger.i(TAG, "Tracking started, id=$trackingId")
                _trackingStarted.value = TrackingStartedEvent(trackingId, project, System.currentTimeMillis())
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error: ${e.code()}", e)
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

    fun addProject(title: String, description: String, color: String) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        val newProject = Project(
            id = 0,
            title = title.trim(),
            description = description.trim().ifEmpty { null },
            color = color.trim().ifEmpty { null },
            trackedThisMonth = 0.0,
            ref = 0,
            archived = false
        )
        AppLogger.i(TAG, "Creating project: title=${newProject.title}")
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.addPersonalProject("Bearer $apiToken", newProject)
                AppLogger.i(TAG, "Project created")
                _projectCreated.value = true
                loadProjects()
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error creating project: ${e.code()}", e)
                _error.value = "Failed to create project (${e.code()})"
                _projectCreated.value = false
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error creating project", e)
                _error.value = "Network error: could not reach the server"
                _projectCreated.value = false
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error creating project", e)
                _error.value = "Failed to create project"
                _projectCreated.value = false
            }
        }
    }

    fun onProjectCreatedHandled() {
        _projectCreated.value = null
    }

    fun archiveProject(project: Project) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Archiving project id=${project.id} title=${project.title}")
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.archiveProject("Bearer $apiToken", ArchiveRequest(project.id, true))
                AppLogger.i(TAG, "Project archived")
                _projectArchived.value = true
                loadProjects()
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error archiving project: ${e.code()}", e)
                _error.value = "Failed to archive project (${e.code()})"
                _projectArchived.value = false
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error archiving project", e)
                _error.value = "Network error: could not reach the server"
                _projectArchived.value = false
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error archiving project", e)
                _error.value = "Failed to archive project"
                _projectArchived.value = false
            }
        }
    }

    fun onProjectArchivedHandled() {
        _projectArchived.value = null
    }
}
