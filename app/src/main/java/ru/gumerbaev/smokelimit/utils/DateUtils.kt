package ru.gumerbaev.smokelimit.utils

import java.util.*

class DateUtils {
    companion object {
        fun toMinutes(timestamp: Long): Int = (timestamp / 1000 / 60).toInt()

        fun remainString(delayMs: Long, timeoutMin: Int): String {
            return minString(timeoutMin - toMinutes(delayMs))
        }

        fun delayString(delayMs: Long): String {
            return minString(toMinutes(delayMs))
        }

        private fun minString(delayMin: Int): String {
            val diffMinutes = delayMin % 60
            val diffHours = delayMin / 60

            val hour = diffHours.toString().padStart(2, '0')
            val min = diffMinutes.toString().padStart(2, '0')

            return "$hour:$min"
        }

        fun dayDiff(date: Date): Int = toDays(System.currentTimeMillis()) - toDays(date.time)
        private fun toDays(timestamp: Long): Int = toMinutes(timestamp) / 60 / 24
    }
}