package de.onemanprojects.klukka

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_ID = "tracking_alerts"
    private const val NOTIF_ID_DURATION = 1001
    private const val NOTIF_ID_TIME = 1002
    private const val NOTIF_ID_AUTOSTOP = 1003
    private const val NOTIF_ID_AUTOSTOPPED = 1004

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showDurationAlert(context: Context, projectName: String, hours: Int) {
        val pendingIntent = buildMainActivityIntent(context, requestCode = 0)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_duration_title))
            .setContentText(context.getString(R.string.notif_duration_text, projectName, hours))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager(context).notify(NOTIF_ID_DURATION, notification)
    }

    fun showTimeAlert(context: Context, projectName: String, timeStr: String) {
        val pendingIntent = buildMainActivityIntent(context, requestCode = 1)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_time_title))
            .setContentText(context.getString(R.string.notif_time_text, projectName, timeStr))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager(context).notify(NOTIF_ID_TIME, notification)
    }

    /** Shown when the autostop threshold is reached while the app is backgrounded.
     *  Tapping "Ignore" cancels the pending autostop; otherwise it fires after [graceSeconds]. */
    fun showAutostopNotification(context: Context, projectName: String, graceSeconds: Int) {
        val ignoreIntent = Intent(context, TrackingAlarmReceiver::class.java).apply {
            action = TrackingAlarmReceiver.ACTION_AUTOSTOP_IGNORE
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context, 2, ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_autostop_title))
            .setContentText(context.getString(R.string.notif_autostop_text, projectName, graceSeconds))
            .setContentIntent(buildMainActivityIntent(context, requestCode = 3))
            .addAction(0, context.getString(R.string.notif_autostop_ignore), ignorePendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        manager(context).notify(NOTIF_ID_AUTOSTOP, notification)
    }

    fun cancelAutostopNotification(context: Context) {
        manager(context).cancel(NOTIF_ID_AUTOSTOP)
    }

    fun showAutostoppedNotification(context: Context, projectName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_autostopped_title))
            .setContentText(context.getString(R.string.notif_autostopped_text, projectName))
            .setContentIntent(buildMainActivityIntent(context, requestCode = 4))
            .setAutoCancel(true)
            .build()
        manager(context).notify(NOTIF_ID_AUTOSTOPPED, notification)
    }

    private fun buildMainActivityIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
