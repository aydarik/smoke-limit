package ru.gumerbaev.smokelimit.data

import android.content.ContentValues
import android.provider.BaseColumns
import android.util.Log
import ru.gumerbaev.smokelimit.entity.SmokeEntity
import java.util.*
import kotlin.collections.ArrayList

class SmokesDbQueryExecutor(private val db: SmokesDbHelper) {
    private val _tag = "SmokesDbQueryExecutor"

    companion object {
        const val SQL_GET_ENTRIES =
            "SELECT * FROM ${SmokesDbContract.SmokeEntry.TABLE_NAME} ORDER BY ${BaseColumns._ID} DESC LIMIT ?"
    }

    fun getLastEntries(limit: Int): List<SmokeEntity> {
        val cursor = db.readableDatabase.rawQuery(SQL_GET_ENTRIES, arrayOf(limit.toString()))
        val result = ArrayList<SmokeEntity>()

        with(cursor) {
            var prev: SmokeEntity? = null
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(BaseColumns._ID))
                val date = getLong(getColumnIndexOrThrow(SmokesDbContract.SmokeEntry.COLUMN_DATE_TITLE))
                val timeout = getInt(getColumnIndexOrThrow(SmokesDbContract.SmokeEntry.COLUMN_TIMEOUT_TITLE))

                val entity = SmokeEntity(id, Date(date), timeout, null)
                prev?.prev = entity

                result.add(entity)
                prev = entity
            }
        }

        return result
    }

    fun addEntry(entry: SmokeEntity) {
        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(SmokesDbContract.SmokeEntry.COLUMN_DATE_TITLE, entry.date.time)
            put(SmokesDbContract.SmokeEntry.COLUMN_TIMEOUT_TITLE, entry.timeout)
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.writableDatabase.insert(SmokesDbContract.SmokeEntry.TABLE_NAME, null, values)
        Log.d(_tag, "Row ID: $newRowId")
    }
}