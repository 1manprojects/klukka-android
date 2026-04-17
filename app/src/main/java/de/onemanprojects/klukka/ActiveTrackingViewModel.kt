package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    // Holds the latest comment text that has not yet been sent to the server
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

    /** Called whenever the comment field changes. Resets the debounce timer. */
    fun onCommentChanged(trackingId: Int, comment: String) {
        pendingComment = comment
        commentDebounceJob?.cancel()
        commentDebounceJob = viewModelScope.launch {
            delay(COMMENT_DEBOUNCE_MS)
            val service = ApiClient.create(secureStorage.getServerUrl())
            flushComment(service, trackingId)
        }
    }

    /** Sends a pending comment update if one exists. No-op if nothing is pending. */
    private suspend fun flushComment(service: ApiService, trackingId: Int) {
        val comment = pendingComment ?: return
        pendingComment = null
        val apiToken = secureStorage.getApiToken()
        try {
            service.updateComment("Bearer $apiToken", CommentUpdate(trackingId, comment))
            AppLogger.i(TAG, "Comment updated for tracking id=$trackingId")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to update comment: ${e.message}")
            // Restore so a subsequent stop can retry
            if (pendingComment == null) pendingComment = comment
        }
    }

    fun stopTracking(trackingId: Int) {
        AppLogger.i(TAG, "Stopping tracking id=$trackingId")
        timerJob?.cancel()
        commentDebounceJob?.cancel()

        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)

                // Flush any unsent comment before stopping
                flushComment(service, trackingId)

                service.stopTracking("Bearer $apiToken", trackingId)
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
                AppLogger.e(TAG, "Network error stopping tracking", e)
                _error.value = "Network error: could not reach the server"
                startTimer(System.currentTimeMillis() - (_elapsedSeconds.value ?: 0L) * 1000L)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error stopping tracking", e)
                _error.value = "Failed to stop tracking"
                startTimer(System.currentTimeMillis() - (_elapsedSeconds.value ?: 0L) * 1000L)
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        commentDebounceJob?.cancel()
        super.onCleared()
    }
}
