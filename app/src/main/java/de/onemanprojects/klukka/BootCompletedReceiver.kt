package de.onemanprojects.klukka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = AppPreferences(context)
        if (prefs.activeTrackingStartTime <= 0L) return

        TrackingAlarmScheduler.cancelAll(context)
        if (prefs.notificationsEnabled) {
            if (prefs.durationAlertEnabled) {
                TrackingAlarmScheduler.scheduleDurationAlarm(context, prefs.activeTrackingStartTime, prefs.durationAlertHours)
            }
            if (prefs.timeAlertEnabled) {
                TrackingAlarmScheduler.scheduleTimeAlarm(context, prefs.timeAlertHour, prefs.timeAlertMinute)
            }
        }
        if (prefs.autostopDurationEnabled) {
            TrackingAlarmScheduler.scheduleAutostopDurationAlarm(context, prefs.activeTrackingStartTime, prefs.autostopDurationHours)
        }
        if (prefs.autostopTimeEnabled) {
            TrackingAlarmScheduler.scheduleAutostopTimeAlarm(context, prefs.autostopTimeHour, prefs.autostopTimeMinute)
        }
        // An autostop warning that fired before reboot would have lost its execute alarm; restart the grace period.
        if (prefs.autostopPending) {
            NotificationHelper.showAutostopNotification(context, prefs.activeTrackingProjectName, TrackingAlarmReceiver.AUTOSTOP_GRACE_SECONDS)
            TrackingAlarmScheduler.scheduleAutostopExecuteAlarm(context, TrackingAlarmReceiver.AUTOSTOP_GRACE_SECONDS)
        }
        AppLogger.d("BootCompletedReceiver", "Alarms rescheduled after boot")
    }
}
