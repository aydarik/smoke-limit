package ru.gumerbaev.smokelimit.utils

import java.util.*

class DateUtils {
    companion object {
        fun toMinutes(timestamp: Long): Int = (timestamp / (1000 * 60)).toInt()

        fun delayString(delayMs: Long): String {
            return minString(toMinutes(delayMs))
        }

        fun minString(delayMin: Int): String {
            val isMinus = delayMin < 0
            val delay = if (isMinus) Math.abs(delayMin) else delayMin

            val diffMinutes = delay % 60
            val diffHours = delay / 60

            val hour = diffHours.toString().padStart(2, '0')
            val min = diffMinutes.toString().padStart(2, '0')

            return "${if (isMinus) "-" else ""}$hour:$min"
        }

        fun dayDiff(date: Date): Int = toDays(System.currentTimeMillis()) - toDays(date.time + TimeZone.getDefault().rawOffset)
        private fun toDays(timestamp: Long): Int = toMinutes(timestamp) / (60 * 24)
    }
}