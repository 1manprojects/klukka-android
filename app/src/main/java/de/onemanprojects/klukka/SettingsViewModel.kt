package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.UserData
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "SettingsViewModel"

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _userData = MutableLiveData<UserData?>()
    val userData: LiveData<UserData?> = _userData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _unauthorized = MutableLiveData<Boolean>()
    val unauthorized: LiveData<Boolean> = _unauthorized

    private val _accountDeleted = MutableLiveData<Boolean>(false)
    val accountDeleted: LiveData<Boolean> = _accountDeleted

    fun loadUserData() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Loading user data")
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val result = service.getUserData("Bearer $apiToken")
                _userData.value = result.payload
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error loading user data: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to load settings (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error loading user data", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading user data", e)
                _error.value = "Failed to load settings"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteToken(tokenId: Int) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Deleting token id=$tokenId")
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.deleteToken("Bearer $apiToken", tokenId)
                loadUserData()
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error deleting token: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to delete token (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error deleting token", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error deleting token", e)
                _error.value = "Failed to delete token"
            }
        }
    }

    fun leaveGroup(groupId: Int) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Leaving group id=$groupId")
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.leaveGroup("Bearer $apiToken", groupId)
                loadUserData()
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error leaving group: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to leave group (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error leaving group", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error leaving group", e)
                _error.value = "Failed to leave group"
            }
        }
    }

    fun deleteAccount() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Deleting account")
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.deleteAccount("Bearer $apiToken")
                secureStorage.clearToken()
                _accountDeleted.value = true
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error deleting account: ${e.code()}", e)
                _error.value = "Failed to delete account (${e.code()})"
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error deleting account", e)
                _error.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error deleting account", e)
                _error.value = "Failed to delete account"
            }
        }
    }
}
