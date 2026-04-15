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

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val result = service.getArchivedProjects("Bearer $apiToken")
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

    fun unarchiveProject(projectId: Int) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.archiveProject("Bearer $apiToken", ArchiveRequest(projectId, archive = false))
                loadArchivedProjects()
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
}
