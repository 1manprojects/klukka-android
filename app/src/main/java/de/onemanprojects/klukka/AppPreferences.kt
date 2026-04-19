package de.onemanprojects.klukka

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var isDebugLogging: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_LOGGING, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_LOGGING, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    companion object {
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
