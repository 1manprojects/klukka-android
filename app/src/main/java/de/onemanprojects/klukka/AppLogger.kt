package de.onemanprojects.klukka

import android.util.Log

/**
 * Application-wide logger.
 * - Debug messages (d) are only emitted when [isDebugEnabled] is true.
 * - Info, warning, and error messages are always emitted.
 */
object AppLogger {

    var isDebugEnabled: Boolean = false

    fun d(tag: String, msg: String) {
        if (isDebugEnabled) Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, msg, throwable)
        else Log.e(tag, msg)
    }
}
