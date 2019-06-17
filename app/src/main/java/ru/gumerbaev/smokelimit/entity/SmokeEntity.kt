package ru.gumerbaev.smokelimit.entity

import ru.gumerbaev.smokelimit.utils.DateUtils
import java.text.DateFormat
import java.util.*

class SmokeEntity(var id: Long?, var date: Date, var timeout: Int, var prev: SmokeEntity?) {
    private val _sdf = DateFormat.getDateTimeInstance()

    constructor(date: Date, timeout: Int) : this(null, date, timeout, null)

    fun getDelayMs(): Long? {
        prev ?: return null
        return date.time - prev!!.date.time
    }

    fun getDateString(): String {
        return _sdf.format(date)
    }

    fun getDelayString(delayMs: Long?): String? {
        delayMs ?: return null
        return DateUtils.delayString(delayMs)
    }
}