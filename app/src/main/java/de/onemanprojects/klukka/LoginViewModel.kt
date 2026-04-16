package de.onemanprojects.klukka

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _urlError = MutableLiveData<String?>()
    val urlError: LiveData<String?> = _urlError

    /** Non-null value = the normalized HTTP URL that triggered the insecure warning. */
    private val _insecureUrlWarning = MutableLiveData<String?>()
    val insecureUrlWarning: LiveData<String?> = _insecureUrlWarning

    private var pendingUrl: String? = null
    private var pendingToken: String? = null

    fun validateAndSave(serverUrl: String, apiToken: String) {
        _urlError.value = null
        val (normalizedUrl, result) = validateUrl(serverUrl)
        when (result) {
            UrlValidationResult.Valid -> saveCredentials(normalizedUrl, apiToken)
            is UrlValidationResult.InsecureHttp -> {
                pendingUrl = normalizedUrl
                pendingToken = apiToken
                _insecureUrlWarning.value = normalizedUrl
            }
            is UrlValidationResult.Invalid -> _urlError.value = result.message
        }
    }

    /** Called when the user accepts the insecure-HTTP warning. */
    fun confirmInsecureUrl() {
        val url = pendingUrl ?: return
        val token = pendingToken ?: return
        pendingUrl = null
        pendingToken = null
        _insecureUrlWarning.value = null
        saveCredentials(url, token)
    }

    /** Called when the user dismisses the insecure-HTTP warning. */
    fun dismissInsecureWarning() {
        pendingUrl = null
        pendingToken = null
        _insecureUrlWarning.value = null
    }

    private fun saveCredentials(serverUrl: String, apiToken: String) {
        secureStorage.saveCredentials(serverUrl, apiToken)
        _saveResult.value = true
    }

    fun getSavedServerUrl(): String = secureStorage.getServerUrl()
    fun getSavedApiToken(): String = secureStorage.getApiToken()

    private fun validateUrl(input: String): Pair<String, UrlValidationResult> {
        var url = input.trim()
        // Append https:// when the user omits the scheme entirely
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return url to UrlValidationResult.Invalid(
                getApplication<Application>().getString(R.string.error_invalid_url)
            )
        }
        return if (url.startsWith("http://")) {
            url to UrlValidationResult.InsecureHttp(url)
        } else {
            url to UrlValidationResult.Valid
        }
    }

    private sealed class UrlValidationResult {
        object Valid : UrlValidationResult()
        data class InsecureHttp(val url: String) : UrlValidationResult()
        data class Invalid(val message: String) : UrlValidationResult()
    }
}
