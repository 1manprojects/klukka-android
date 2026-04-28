package de.onemanprojects.klukka

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonPrimitive
import de.onemanprojects.klukka.model.CommentUpdate
import de.onemanprojects.klukka.network.ApiClient
import de.onemanprojects.klukka.network.ApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "ActiveTrackingVM"
private const val COMMENT_DEBOUNCE_MS = 1000L

class ActiveTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)
    private val offlineCache = OfflineCache(application)

    private val _elapsedSeconds = MutableLiveData(0L)
    val elapsedSeconds: LiveData<Long> = _elapsedSeconds

    private val _trackingStopped = MutableLiveData<Boolean>()
    val trackingStopped: LiveData<Boolean> = _trackingStopped

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _unauthorized = MutableLiveData<Boolean>()
    val unauthorized: LiveData<Boolean> = _unauthorized

    private var timerJob: Job? = null
    private var commentDebounceJob: Job? = null

    private var pendingComment: String? = null

    fun startTimer(startTime: Long) {
        val nowMs = System.currentTimeMillis()
        val initialElapsed = (nowMs - startTime) / 1000L
        AppLogger.i(TAG, "Timer started: startTime=$startTime now=$nowMs initialElapsed=${initialElapsed}s")
        timerJob?.cancel()
        _elapsedSeconds.value = initialElapsed
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value = (System.currentTimeMillis() - startTime) / 1000L
            }
        }
    }

    fun onCommentChanged(trackingId: Int, comment: String) {
        pendingComment = comment
        if (trackingId == -1) {
            // Offline session: persist comment to cache only
            offlineCache.updatePendingComment(comment)
            return
        }
        commentDebounceJob?.cancel()
        commentDebounceJob = viewModelScope.launch {
            delay(COMMENT_DEBOUNCE_MS)
            val service = ApiClient.create(secureStorage.getServerUrl())
            flushComment(service, trackingId)
        }
    }

    private suspend fun flushComment(service: ApiService, trackingId: Int) {
        if (trackingId == -1) return
        val comment = pendingComment ?: return
        pendingComment = null
        val apiToken = secureStorage.getApiToken()
        try {
            service.updateComment("Bearer $apiToken", CommentUpdate(trackingId, comment))
            AppLogger.i(TAG, "Comment updated for tracking id=$trackingId")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to update comment: ${e.message}")
            if (pendingComment == null) pendingComment = comment
        }
    }

    fun stopTracking(trackingId: Int, projectId: Int) {
        AppLogger.i(TAG, "Stopping tracking id=$trackingId")
        timerJob?.cancel()
        commentDebounceJob?.cancel()

        val endTimeMs = System.currentTimeMillis()
        val finalComment = pendingComment ?: ""

        if (trackingId == -1) {
            // Session was started offline — convert OfflineStart to OfflineStartAndStop in cache
            offlineCache.convertOfflineStartToStartStop(endTimeMs, finalComment)
            AppLogger.i(TAG, "Offline: queued stop for offline session")
            _trackingStopped.value = true
            return
        }

        if (!isOnline()) {
            // Online session but currently offline — queue the stop for later
            offlineCache.addPendingAction(
                PendingTrackingAction.OnlineStop(trackingId, projectId, endTimeMs, finalComment)
            )
            AppLogger.i(TAG, "Offline: queued stop for online tracking id=$trackingId")
            _trackingStopped.value = true
            return
        }

        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                flushComment(service, trackingId)
                service.stopTracking("Bearer $apiToken", JsonPrimitive(trackingId))
                AppLogger.i(TAG, "Tracking stopped id=$trackingId")
                _trackingStopped.value = true
            } catch (e: HttpException) {
                AppLogger.e(TAG, "HTTP error stopping tracking: ${e.code()}", e)
                if (e.code() == 401) {
                    secureStorage.clearToken()
                    _unauthorized.value = true
                } else {
                    _error.value = "Failed to stop tracking — server error (${e.code()})"
                    startTimer(System.currentTimeMillis() - (_elapsedSeconds.value ?: 0L) * 1000L)
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network error stopping tracking — queuing offline stop", e)
                offlineCache.addPendingAction(
                    PendingTrackingAction.OnlineStop(trackingId, projectId, endTimeMs, finalComment)
                )
                _trackingStopped.value = true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error stopping tracking", e)
                _error.value = "Failed to stop tracking"
                startTimer(System.currentTimeMillis() - (_elapsedSeconds.value ?: 0L) * 1000L)
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        timerJob?.cancel()
        commentDebounceJob?.cancel()
        super.onCleared()
    }
}
