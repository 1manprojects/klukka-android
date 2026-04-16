package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.ArchiveRequest
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "ArchivedProjectsVM"

class ArchivedProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _unauthorized = MutableLiveData<Boolean>()
    val unauthorized: LiveData<Boolean> = _unauthorized

    fun loadArchivedProjects() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        AppLogger.i(TAG, "Loading archived projects from $serverUrl")
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val result = service.getArchivedProjects("Bearer $apiToken")
                val allProjects = (result.own ?: emptyList()) + (result.group ?: emptyList())
                AppLogger.i(TAG, "Loaded ${allProjects.size} archived projects")
                _projects.value = allProjects
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error loading archived projects: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to load archived projects — server error (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error loading archived projects", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading archived projects", e)
                _error.value = "Failed to load archived projects"
            } finally {
                _loading.value = false
            }
        }
    }

    fun unarchiveProject(projectId: Int) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        AppLogger.i(TAG, "Unarchiving project id=$projectId")

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.archiveProject("Bearer $apiToken", ArchiveRequest(projectId, archive = false))
                AppLogger.i(TAG, "Project $projectId unarchived, reloading list")
                loadArchivedProjects()
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error unarchiving project: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to unarchive project — server error (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error unarchiving project", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error unarchiving project", e)
                _error.value = "Failed to unarchive project"
            }
        }
    }
}
