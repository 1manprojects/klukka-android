package de.onemanprojects.klukka

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecureStorage(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "klukka_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_TOKEN = "api_token"
    }

    fun saveCredentials(serverUrl: String, apiToken: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_API_TOKEN, apiToken)
            .apply()
    }

    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, "") ?: ""

    fun getApiToken(): String = prefs.getString(KEY_API_TOKEN, "") ?: ""

    fun clearToken() {
        prefs.edit().remove(KEY_API_TOKEN).apply()
    }

    fun hasCredentials(): Boolean = getServerUrl().isNotEmpty() && getApiToken().isNotEmpty()
}
