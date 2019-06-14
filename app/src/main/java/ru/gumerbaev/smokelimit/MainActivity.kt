package ru.gumerbaev.smokelimit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.gumerbaev.smokelimit.data.SmokesDbContract
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.utils.DateUtils
import java.text.DateFormat
import java.util.*
import java.util.stream.Collectors

class MainActivity : AppCompatActivity() {
    private val _tag = "MyActivity"
    private val _sdf = DateFormat.getDateTimeInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val curr = sharedPref.getInt(
            getString(R.string.current_timeout_key),
            resources.getInteger(R.integer.current_timeout_default_key)
        )
        val inc = sharedPref.getInt(
            getString(R.string.increase_timeout_key),
            resources.getInteger(R.integer.increase_timeout_default_key)
        )

        currTimeoutTextBox.value = curr
        currTimeoutTextBox.setOnValueChangedListener { _, _, newVal ->
            with(sharedPref.edit()) {
                putInt(getString(R.string.current_timeout_key), newVal)
                apply()
            }
        }

        increaseTimeoutTextBox.value = inc
        increaseTimeoutTextBox.setOnValueChangedListener { _, _, newVal ->
            with(sharedPref.edit()) {
                putInt(getString(R.string.increase_timeout_key), newVal)
                apply()
            }
        }

        justSmokedButton.setOnClickListener { insertSmokeEntry() }
        loadLastEvents(curr)
    }

    @SuppressLint("SetTextI18n")
    private fun loadLastEvents(currTimeout: Int) {
        val db = SmokesDbHelper(this).readableDatabase

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(
            BaseColumns._ID,
            SmokesDbContract.SmokeEntry.COLUMN_DATE_TITLE,
            SmokesDbContract.SmokeEntry.COLUMN_TIMEOUT_TITLE
        )

        // How you want the results sorted in the resulting Cursor
        val sortOrder = "${SmokesDbContract.SmokeEntry.COLUMN_DATE_TITLE} ASC"

        val cursor = db.query(
            SmokesDbContract.SmokeEntry.TABLE_NAME, // The table to query
            projection,         // The array of columns to return (pass null to get all)
            null,       // The columns for the WHERE clause
            null,   // The values for the WHERE clause
            null,       // don't group the rows
            null,       // don't filter by row groups
            sortOrder           // The sort order
        )

        val minString = getString(R.string.minutes)

        val historyArray = ArrayDeque<String>()
        var prevDate: Long? = null
        with(cursor) {
            while (moveToNext()) {
                val date = getLong(getColumnIndexOrThrow(SmokesDbContract.SmokeEntry.COLUMN_DATE_TITLE))
                val timeout = getInt(getColumnIndexOrThrow(SmokesDbContract.SmokeEntry.COLUMN_TIMEOUT_TITLE))
                if (prevDate != null) {
                    val realTimeout = DateUtils.toMinutes(date - prevDate!!)
                    historyArray.push(_sdf.format(Date(date)) + " - " + realTimeout + " (" + timeout + ") " + minString)
                } else {
                    historyArray.push(_sdf.format(Date(date)))
                }
                prevDate = date
            }
        }

        if (prevDate != null) {
            val realTimeout = DateUtils.toMinutes(System.currentTimeMillis() - prevDate!!)
            val accessible = if (historyArray.isEmpty()) {
                true
            } else {
                historyList.setText(
                    historyArray.stream()
                        .collect(Collectors.joining("\n"))
                )

                realTimeout >= currTimeout
            }

            if (accessible) justSmokedButton.text = getString(R.string.just_smoked)
            else justSmokedButton.text = (currTimeout - realTimeout).toString() + " " + minString

            justSmokedButton.isEnabled = accessible
        } else justSmokedButton.isEnabled = true
    }

    private fun insertSmokeEntry() {
        val currTime = System.currentTimeMillis()

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val lastSmokeTime = sharedPref.getLong(
            getString(R.string.last_smoke_time_key),
            0
        )

        var currTimeout = sharedPref.getInt(
            getString(R.string.current_timeout_key),
            resources.getInteger(R.integer.current_timeout_default_key)
        )

        // Gets the data repository in write mode
        val db = SmokesDbHelper(this).writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(SmokesDbContract.SmokeEntry.COLUMN_DATE_TITLE, currTime)
            put(SmokesDbContract.SmokeEntry.COLUMN_TIMEOUT_TITLE, currTimeout)
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db?.insert(SmokesDbContract.SmokeEntry.TABLE_NAME, null, values)
        Log.d(_tag, "Row ID: $newRowId")

        with(sharedPref.edit()) {
            putLong(getString(R.string.last_smoke_time_key), currTime)
            apply()
        }

        if (lastSmokeTime > 0) {
            val lastSmokeDay = DateUtils.toDays(lastSmokeTime)
            val currDay = DateUtils.toDays(currTime)
            if (lastSmokeDay < currDay)
                currTimeout = increaseTimeout(sharedPref, currTimeout, (currDay - lastSmokeDay).toInt())
        }

        loadLastEvents(currTimeout)
    }

    private fun increaseTimeout(sharedPref: SharedPreferences, currTimeout: Int, incDays: Int): Int {
        val inc = sharedPref.getInt(
            getString(R.string.increase_timeout_key),
            resources.getInteger(R.integer.increase_timeout_default_key)
        )

        val maxTimeout = resources.getInteger(R.integer.max_timeout_default_key)
        var increasedTimeout = currTimeout + (inc * incDays)
        if (increasedTimeout > maxTimeout) {
            increasedTimeout = maxTimeout
        }

        with(sharedPref.edit()) {
            putInt(getString(R.string.current_timeout_key), increasedTimeout)
            apply()
        }

        currTimeoutTextBox.value = increasedTimeout
        return increasedTimeout
    }
}