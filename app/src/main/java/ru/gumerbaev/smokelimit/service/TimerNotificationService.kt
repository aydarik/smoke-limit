package ru.gumerbaev.smokelimit.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import ru.gumerbaev.smokelimit.utils.DateUtils

class TimerNotificationService : Service() {

    companion object {
        const val TAG = "TimerNotificationService"

        fun getIntent(context: Context): Intent {
            return Intent(context, TimerNotificationService::class.java)
        }
    }

    internal inner class TimeBinder : Binder() {
        fun setParams(lastSmokeTime: Long?, currTimeout: Int) {
            _lastSmokeTime = lastSmokeTime
            _currTimeout = currTimeout
        }

/*
        fun getRemainString(): String {
            val curr = System.currentTimeMillis()
            val realTimeoutMs = curr - (_lastSmokeTime ?: curr)
            return DateUtils.remainString(realTimeoutMs, _currTimeout)
        }
*/

        fun getRemain(): Int {
            val curr = System.currentTimeMillis()
            val realTimeoutMs = curr - (_lastSmokeTime ?: curr)
            return DateUtils.toMinutes(realTimeoutMs) - _currTimeout;
        }
    }

    private val _binder = TimeBinder()

    private var _lastSmokeTime: Long? = null
    private var _currTimeout: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound")
        return _binder
    }
}