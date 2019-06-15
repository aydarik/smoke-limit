package ru.gumerbaev.smokelimit.entity

import ru.gumerbaev.smokelimit.utils.DateUtils
import java.text.DateFormat
import java.util.*

class SmokeEntity(var id: Long?, var date: Date, var timeout: Int, var prev: SmokeEntity?) {
    private val _sdf = DateFormat.getDateTimeInstance()

    constructor(date: Date, timeout: Int) : this(null, date, timeout, null)

    private fun getDelay(): Long? {
        if (prev == null) return null
        return date.time - prev!!.date.time
    }

    fun getDateString(): String {
        return _sdf.format(date)
    }

    fun getDelayString(): String? {
        val delay = getDelay() ?: return "-"
        return DateUtils.delayString(delay)
    }
}