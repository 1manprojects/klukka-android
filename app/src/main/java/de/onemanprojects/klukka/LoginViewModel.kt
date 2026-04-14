package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    fun saveCredentials(serverUrl: String, apiToken: String) {
        secureStorage.saveCredentials(serverUrl, apiToken)
        _saveResult.value = true
    }

    fun getSavedServerUrl(): String = secureStorage.getServerUrl()

    fun getSavedApiToken(): String = secureStorage.getApiToken()
}
