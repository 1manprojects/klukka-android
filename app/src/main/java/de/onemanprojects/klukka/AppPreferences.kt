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

    // --- Notification preferences ---

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var durationAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_DURATION_ALERT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DURATION_ALERT_ENABLED, value).apply()

    var durationAlertHours: Int
        get() = prefs.getInt(KEY_DURATION_ALERT_HOURS, 8)
        set(value) = prefs.edit().putInt(KEY_DURATION_ALERT_HOURS, value).apply()

    var timeAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIME_ALERT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TIME_ALERT_ENABLED, value).apply()

    var timeAlertHour: Int
        get() = prefs.getInt(KEY_TIME_ALERT_HOUR, 19)
        set(value) = prefs.edit().putInt(KEY_TIME_ALERT_HOUR, value).apply()

    var timeAlertMinute: Int
        get() = prefs.getInt(KEY_TIME_ALERT_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_TIME_ALERT_MINUTE, value).apply()

    // Tracking state cached for BroadcastReceiver access (no ViewModel available there)
    var activeTrackingStartTime: Long
        get() = prefs.getLong(KEY_ACTIVE_TRACKING_START, 0L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_TRACKING_START, value).apply()

    var activeTrackingProjectName: String
        get() = prefs.getString(KEY_ACTIVE_TRACKING_PROJECT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_TRACKING_PROJECT, value).apply()

    companion object {
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DURATION_ALERT_ENABLED = "duration_alert_enabled"
        private const val KEY_DURATION_ALERT_HOURS = "duration_alert_hours"
        private const val KEY_TIME_ALERT_ENABLED = "time_alert_enabled"
        private const val KEY_TIME_ALERT_HOUR = "time_alert_hour"
        private const val KEY_TIME_ALERT_MINUTE = "time_alert_minute"
        private const val KEY_ACTIVE_TRACKING_START = "active_tracking_start"
        private const val KEY_ACTIVE_TRACKING_PROJECT = "active_tracking_project"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}
