package de.onemanprojects.klukka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TrackingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AppPreferences(context)
        if (!prefs.notificationsEnabled) return
        if (prefs.activeTrackingStartTime <= 0L) return

        val projectName = prefs.activeTrackingProjectName

        when (intent.getStringExtra(EXTRA_ALARM_TYPE)) {
            ALARM_DURATION -> {
                if (prefs.durationAlertEnabled) {
                    NotificationHelper.showDurationAlert(context, projectName, prefs.durationAlertHours)
                }
            }
            ALARM_TIME -> {
                if (prefs.timeAlertEnabled) {
                    val timeStr = String.format("%02d:%02d", prefs.timeAlertHour, prefs.timeAlertMinute)
                    NotificationHelper.showTimeAlert(context, projectName, timeStr)
                }
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val ALARM_DURATION = "duration"
        const val ALARM_TIME = "time"
    }
}
