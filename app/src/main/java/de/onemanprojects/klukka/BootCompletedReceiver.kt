package de.onemanprojects.klukka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = AppPreferences(context)
        if (!prefs.notificationsEnabled) return
        if (prefs.activeTrackingStartTime <= 0L) return

        TrackingAlarmScheduler.cancelAll(context)
        if (prefs.durationAlertEnabled) {
            TrackingAlarmScheduler.scheduleDurationAlarm(context, prefs.activeTrackingStartTime, prefs.durationAlertHours)
        }
        if (prefs.timeAlertEnabled) {
            TrackingAlarmScheduler.scheduleTimeAlarm(context, prefs.timeAlertHour, prefs.timeAlertMinute)
        }
        AppLogger.d("BootCompletedReceiver", "Alarms rescheduled after boot")
    }
}
