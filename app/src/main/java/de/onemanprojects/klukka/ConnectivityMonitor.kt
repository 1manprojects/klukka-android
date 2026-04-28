package de.onemanprojects.klukka

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.MutableLiveData

class ConnectivityMonitor(context: Context) {

    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    val isOnline = MutableLiveData(checkNow())

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { isOnline.postValue(true) }
        override fun onLost(network: Network) { isOnline.postValue(checkNow()) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            isOnline.postValue(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    fun checkNow(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun unregister() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }
}
