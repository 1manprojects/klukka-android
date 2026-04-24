package de.onemanprojects.klukka

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var isDebugLogging: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_LOGGING, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_LOGGING, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    companion object {
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_THEME_MODE = "theme_mode"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}
