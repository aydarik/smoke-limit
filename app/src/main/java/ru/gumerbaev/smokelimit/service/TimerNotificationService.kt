package ru.gumerbaev.smokelimit.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import ru.gumerbaev.smokelimit.R
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.data.SmokesDbQueryExecutor
import ru.gumerbaev.smokelimit.utils.DateUtils

class TimerNotificationService : Service() {

    companion object {
        const val TAG = "TimerNotificationService"

        fun getIntent(context: Context): Intent {
            return Intent(context, TimerNotificationService::class.java)
        }
    }

    internal inner class TimeBinder(private val context: Context) : Binder() {
        fun getRemain(): Int? {
            val lastSmokeTime = _dbExecutor.getLastEntries(1).firstOrNull()?.date?.time ?: return null

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val currTimeout = sharedPref.getInt(
                getString(R.string.current_timeout_key),
                resources.getInteger(R.integer.current_timeout_default_key)
            )

            val curr = System.currentTimeMillis()
            val realTimeoutMs = curr - lastSmokeTime
            return DateUtils.toMinutes(realTimeoutMs) - currTimeout
        }
    }

    private val _binder = TimeBinder(this)
    private val _dbExecutor = SmokesDbQueryExecutor(SmokesDbHelper(this))

    override fun onCreate() {
        Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show()

/*
        val context = this.applicationContext;
        val threadHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                val remain = _binder.getRemain()
                if (remain != null)
                    Toast.makeText(context, DateUtils.minString(remain), Toast.LENGTH_LONG).show()
            }
        }

        fixedRateTimer(
            "time_check", true, Date(), 1000 * 60
        ) {
            threadHandler.obtainMessage().sendToTarget()
        }
*/
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound")
        return _binder
    }
}