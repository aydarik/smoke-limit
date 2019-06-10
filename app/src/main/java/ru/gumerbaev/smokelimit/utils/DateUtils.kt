package ru.gumerbaev.smokelimit.utils

class DateUtils {

    companion object {
        fun toMinutes(timestamp: Long): Long = timestamp / 1000 / 60
        fun toDays(timestamp: Long): Long = toMinutes(timestamp) / 60 / 24
    }
}