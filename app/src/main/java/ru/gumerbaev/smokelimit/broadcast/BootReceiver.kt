package ru.gumerbaev.smokelimit.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.gumerbaev.smokelimit.service.TimerNotificationService

class BootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val timerIntent = Intent(context, TimerNotificationService::class.java)
        context?.startService(timerIntent)
    }
}