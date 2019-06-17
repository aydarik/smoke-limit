package ru.gumerbaev.smokelimit.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import ru.gumerbaev.smokelimit.MainActivity
import ru.gumerbaev.smokelimit.R
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.data.SmokesDbQueryExecutor
import ru.gumerbaev.smokelimit.utils.DateUtils
import java.util.*
import kotlin.concurrent.fixedRateTimer

class TimerNotificationService : Service() {

    companion object {
        const val TAG = "TimerNotificationService"
        const val CHANNEL_ID = "SMOKE_CHANNEL"
        const val NOTIFICATION_ID = 1

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

    private var started = false
    private var _notificationBuilder: Notification.Builder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (started) return super.onStartCommand(intent, flags, startId)

        started = true
        _notificationBuilder = createNotificationChannel()
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
*/

        fixedRateTimer(
            "time_check", true, Date(), 1000 * 60
        ) {
            // threadHandler.obtainMessage().sendToTarget()
            val remain = _binder.getRemain()
            if (remain != null)
                addNotification(DateUtils.minString(remain))
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound")
        return _binder
    }

    private fun addNotification(text: String) {
        with(_notificationBuilder) {
            this?.setContentText(text)
            if (text.startsWith('-')) this?.setSmallIcon(android.R.drawable.ic_dialog_alert)
            else this?.setSmallIcon(android.R.drawable.ic_dialog_info)
        }

        val notification = _notificationBuilder?.build() ?: return
        notification.flags = Notification.FLAG_NO_CLEAR

        // Send the notification
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel(): Notification.Builder {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, "timeout", importance).apply {}
            // Register the channel with the system
            with(NotificationManagerCompat.from(this)) {
                createNotificationChannel(channel)
            }
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))

        // Create the pending intent and add to the notification
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        builder.setContentIntent(pendingIntent)

        return builder
    }
}