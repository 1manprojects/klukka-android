package de.onemanprojects.klukka

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object TrackingAlarmScheduler {

    private const val REQUEST_DURATION = 100
    private const val REQUEST_TIME = 101

    fun scheduleDurationAlarm(context: Context, startTimeMillis: Long, hours: Int) {
        val triggerAt = startTimeMillis + hours * 3600L * 1000L
        // If duration already elapsed (e.g. after app restart), fire immediately
        val scheduleAt = if (triggerAt <= System.currentTimeMillis()) System.currentTimeMillis() + 1_000L else triggerAt
        val pendingIntent = buildPendingIntent(context, REQUEST_DURATION, TrackingAlarmReceiver.ALARM_DURATION)
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduleAt, pendingIntent)
        AppLogger.d("TrackingAlarmScheduler", "Duration alarm set for +${hours}h from start")
    }

    fun scheduleTimeAlarm(context: Context, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If the time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val pendingIntent = buildPendingIntent(context, REQUEST_TIME, TrackingAlarmReceiver.ALARM_TIME)
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        AppLogger.d("TrackingAlarmScheduler", "Time alarm set for $hour:$minute")
    }

    fun cancelAll(context: Context) {
        cancelPendingIntent(context, REQUEST_DURATION, TrackingAlarmReceiver.ALARM_DURATION)
        cancelPendingIntent(context, REQUEST_TIME, TrackingAlarmReceiver.ALARM_TIME)
        AppLogger.d("TrackingAlarmScheduler", "All alarms cancelled")
    }

    private fun buildPendingIntent(context: Context, requestCode: Int, alarmType: String): PendingIntent {
        val intent = Intent(context, TrackingAlarmReceiver::class.java).apply {
            putExtra(TrackingAlarmReceiver.EXTRA_ALARM_TYPE, alarmType)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPendingIntent(context: Context, requestCode: Int, alarmType: String) {
        val intent = Intent(context, TrackingAlarmReceiver::class.java).apply {
            putExtra(TrackingAlarmReceiver.EXTRA_ALARM_TYPE, alarmType)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager(context).cancel(pi)
    }

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}
