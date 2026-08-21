package de.onemanprojects.klukka

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.JsonPrimitive
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_AUTOSTOP_IGNORE) {
            handleAutostopIgnore(context)
            return
        }

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
                    handleAutostopWarning(context, prefs, projectName)
                }
            }
            ALARM_AUTOSTOP_TIME -> {
                if (prefs.autostopTimeEnabled) {
                    handleAutostopWarning(context, prefs, projectName)
                }
            }
            ALARM_AUTOSTOP_EXECUTE -> {
                performBackgroundAutostop(context, prefs, projectName)
            }
        }
    }

    /** Fired when a configured autostop threshold is reached. If the app is in the foreground,
     *  ActiveTrackingFragment shows a countdown dialog. Otherwise a notification is shown and
     *  tracking is actually stopped after [AUTOSTOP_GRACE_SECONDS] unless the user taps Ignore. */
    private fun handleAutostopWarning(context: Context, prefs: AppPreferences, projectName: String) {
        prefs.autostopPending = true
        context.sendBroadcast(Intent(ACTION_AUTOSTOP_WARNING).setPackage(context.packageName))

        if (!isAppInForeground(context)) {
            NotificationHelper.showAutostopNotification(context, projectName, AUTOSTOP_GRACE_SECONDS)
            TrackingAlarmScheduler.scheduleAutostopExecuteAlarm(context, AUTOSTOP_GRACE_SECONDS)
        }
    }

    private fun handleAutostopIgnore(context: Context) {
        val prefs = AppPreferences(context)
        prefs.autostopPending = false
        TrackingAlarmScheduler.cancelAutostopExecuteAlarm(context)
        NotificationHelper.cancelAutostopNotification(context)
    }

    /** Stops tracking via the API without any running Activity/ViewModel. Used when the
     *  autostop grace period elapses while the app is backgrounded. */
    private fun performBackgroundAutostop(context: Context, prefs: AppPreferences, projectName: String) {
        val trackingId = prefs.activeTrackingId
        NotificationHelper.cancelAutostopNotification(context)
        prefs.autostopPending = false

        if (trackingId < 0) return

        val secureStorage = SecureStorage(context)
        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()
        if (serverUrl.isEmpty() || apiToken.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.stopTracking("Bearer $apiToken", JsonPrimitive(trackingId))
                AppLogger.i("TrackingAlarmReceiver", "Background autostop: tracking $trackingId stopped")
                prefs.activeTrackingStartTime = 0L
                prefs.activeTrackingProjectName = ""
                prefs.activeTrackingId = -1
                TrackingAlarmScheduler.cancelAll(context)
                NotificationHelper.showAutostoppedNotification(context, projectName)
            } catch (e: Exception) {
                AppLogger.w("TrackingAlarmReceiver", "Background autostop failed, retrying later: ${e.message}")
                TrackingAlarmScheduler.scheduleAutostopExecuteAlarm(context, AUTOSTOP_RETRY_SECONDS)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = context.packageName
        return am.runningAppProcesses.orEmpty().any {
            it.processName == packageName &&
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    companion object {
        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val ALARM_DURATION = "duration"
        const val ALARM_TIME = "time"
        const val ALARM_AUTOSTOP_DURATION = "autostop_duration"
        const val ALARM_AUTOSTOP_TIME = "autostop_time"
        const val ALARM_AUTOSTOP_EXECUTE = "autostop_execute"
        const val ACTION_AUTOSTOP_WARNING = "de.onemanprojects.klukka.AUTOSTOP_WARNING"
        const val ACTION_AUTOSTOP_IGNORE = "de.onemanprojects.klukka.AUTOSTOP_IGNORE"

        /** Grace period between an autostop warning and tracking actually stopping,
         *  used both by the foreground dialog countdown and the background notification. */
        const val AUTOSTOP_GRACE_SECONDS = 30
        const val AUTOSTOP_RETRY_SECONDS = 60
    }
}
