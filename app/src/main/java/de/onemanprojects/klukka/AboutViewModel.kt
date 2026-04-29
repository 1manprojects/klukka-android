package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.launch

private const val TAG = "AboutViewModel"

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _backendVersion = MutableLiveData<String?>()
    val backendVersion: LiveData<String?> = _backendVersion

    fun loadServerInfo() {
        val serverUrl = secureStorage.getServerUrl()
        if (serverUrl.isEmpty()) return
        viewModelScope.launch {
            try {
                val info = ApiClient.create(serverUrl).getServerInfo().payload
                _backendVersion.value = info?.version
                AppLogger.d(TAG, "Backend version: ${info?.version}")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not load server info: ${e.message}")
                _backendVersion.value = null
            }
        }
    }
}
