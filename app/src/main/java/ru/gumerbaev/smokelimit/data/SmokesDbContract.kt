package ru.gumerbaev.smokelimit.data

import android.provider.BaseColumns

object SmokesDbContract {
    // Table contents are grouped together in an anonymous object.
    object SmokeEntry : BaseColumns {
        const val TABLE_NAME = "smokes"
        const val COLUMN_DATE_TITLE = "date"
        const val COLUMN_TIMEOUT_TITLE = "timeout"
    }
}