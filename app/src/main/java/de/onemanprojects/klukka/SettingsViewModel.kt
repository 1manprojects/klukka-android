package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.model.ExportUserData
import de.onemanprojects.klukka.model.UserData
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.nio.charset.StandardCharsets

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

    private val _loggedOut = MutableLiveData<Boolean>(false)
    val loggedOut: LiveData<Boolean> = _loggedOut

    private val _exportBytes = MutableLiveData<ByteArray?>()
    val exportBytes: LiveData<ByteArray?> = _exportBytes

    private val _exportLoading = MutableLiveData(false)
    val exportLoading: LiveData<Boolean> = _exportLoading

    private val _exportError = MutableLiveData<String?>()
    val exportError: LiveData<String?> = _exportError

    // null = idle, true = success, false = error
    private val _importResult = MutableLiveData<Boolean?>(null)
    val importResult: LiveData<Boolean?> = _importResult

    private val _importLoading = MutableLiveData(false)
    val importLoading: LiveData<Boolean> = _importLoading

    private val _importError = MutableLiveData<String?>()
    val importError: LiveData<String?> = _importError

    private val gson = GsonBuilder().setPrettyPrinting().create()

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
                service.deleteToken("Bearer $apiToken", JsonPrimitive(tokenId))
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

    fun leaveGroup(group: de.onemanprojects.klukka.model.Group) {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Leaving group id=${group.id}")
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.leaveGroup("Bearer $apiToken", group)
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

    fun logout() {
        AppLogger.i(TAG, "Logging out")
        secureStorage.clearToken()
        _loggedOut.value = true
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

    fun exportUserData() {
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        AppLogger.i(TAG, "Exporting user data")
        _exportLoading.value = true
        _exportError.value = null
        _exportBytes.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                val result = service.exportUserData("Bearer $apiToken")
                val json = gson.toJson(result.payload ?: ExportUserData(emptyList(), emptyList()))
                _exportBytes.value = json.toByteArray(StandardCharsets.UTF_8)
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error exporting user data: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _exportError.value = "Export failed (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error exporting user data", e)
                _exportError.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error exporting user data", e)
                _exportError.value = "Export failed"
            } finally {
                _exportLoading.value = false
            }
        }
    }

    fun clearExportBytes() {
        _exportBytes.value = null
    }

    fun importUserData(json: String) {
        AppLogger.i(TAG, "Importing user data")
        val data = try {
            gson.fromJson(json, ExportUserData::class.java)
        } catch (e: JsonParseException) {
            AppLogger.e(TAG, "Invalid import file", e)
            _importError.value = "This file is not a valid Klukka export"
            return
        }
        if (data == null) {
            _importError.value = "This file is not a valid Klukka export"
            return
        }
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        _importLoading.value = true
        _importError.value = null
        _importResult.value = null
        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.importUserData("Bearer $apiToken", data)
                _importResult.value = true
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error importing user data: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _importError.value = "Import failed (${e.code()})"
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error importing user data", e)
                _importError.value = "Network error: could not reach the server"
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error importing user data", e)
                _importError.value = "Import failed"
            } finally {
                _importLoading.value = false
            }
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }
}
