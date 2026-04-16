package de.onemanprojects.klukka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.onemanprojects.klukka.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "ActiveTrackingVM"

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

    fun startTimer(startTime: Long) {
        AppLogger.d(TAG, "Timer started, startTime=$startTime")
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000L
                _elapsedSeconds.postValue(elapsed)
                delay(1000)
            }
        }
    }

    fun stopTracking(trackingId: Int) {
        AppLogger.d(TAG, "Stopping tracking id=$trackingId")
        timerJob?.cancel()

        val serverUrl = secureStorage.getServerUrl()
        val apiToken = secureStorage.getApiToken()

        viewModelScope.launch {
            try {
                val service = ApiClient.create(serverUrl)
                service.stopTracking("Bearer $apiToken", trackingId)
                AppLogger.d(TAG, "Tracking stopped id=$trackingId")
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
        super.onCleared()
    }
}
