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
