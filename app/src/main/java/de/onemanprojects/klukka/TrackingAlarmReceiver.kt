package de.onemanprojects.klukka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TrackingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AppPreferences(context)
        if (prefs.activeTrackingStartTime <= 0L) return

        val projectName = prefs.activeTrackingProjectName

        when (intent.getStringExtra(EXTRA_ALARM_TYPE)) {
            ALARM_DURATION -> {
                if (prefs.notificationsEnabled && prefs.durationAlertEnabled) {
                    NotificationHelper.showDurationAlert(context, projectName, prefs.durationAlertHours)
                }
            }
            ALARM_TIME -> {
                if (prefs.notificationsEnabled && prefs.timeAlertEnabled) {
                    val timeStr = String.format("%02d:%02d", prefs.timeAlertHour, prefs.timeAlertMinute)
                    NotificationHelper.showTimeAlert(context, projectName, timeStr)
                }
            }
            ALARM_AUTOSTOP_DURATION -> {
                if (prefs.autostopDurationEnabled) {
                    prefs.autostopPending = true
                    context.sendBroadcast(Intent(ACTION_AUTOSTOP_WARNING).setPackage(context.packageName))
                }
            }
            ALARM_AUTOSTOP_TIME -> {
                if (prefs.autostopTimeEnabled) {
                    prefs.autostopPending = true
                    context.sendBroadcast(Intent(ACTION_AUTOSTOP_WARNING).setPackage(context.packageName))
                }
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val ALARM_DURATION = "duration"
        const val ALARM_TIME = "time"
        const val ALARM_AUTOSTOP_DURATION = "autostop_duration"
        const val ALARM_AUTOSTOP_TIME = "autostop_time"
        const val ACTION_AUTOSTOP_WARNING = "de.onemanprojects.klukka.AUTOSTOP_WARNING"
    }
}
