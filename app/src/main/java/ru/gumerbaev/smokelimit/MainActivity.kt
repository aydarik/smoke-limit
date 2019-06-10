package ru.gumerbaev.smokelimit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.gumerbaev.smokelimit.data.SmokesDbContract
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.utils.DateUtils
import java.text.DateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

class MainActivity : AppCompatActivity() {
    private val logger = Logger.getLogger("MainActivity")
    private val sdf = DateFormat.getDateTimeInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val aim = sharedPref.getInt(
            getString(R.string.smoke_limit_aim_key),
            resources.getInteger(R.integer.smoke_limit_aim_default_key)
        )
        val curr = sharedPref.getInt(
            getString(R.string.current_timeout_key),
            resources.getInteger(R.integer.current_timeout_default_key)
        )
        val inc = sharedPref.getInt(
            getString(R.string.increase_timeout_key),
            resources.getInteger(R.integer.increase_timeout_default_key)
        )

        smokeLimitAimTextBox.setText(aim.toString())
        smokeLimitAimTextBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length != 0) {
                    with(sharedPref.edit()) {
                        putInt(getString(R.string.current_timeout_key), Integer.valueOf(s.toString()))
                        apply()
                    }
                }
            }
        })
        smokeLimitAimTextBox.isEnabled = false // TODO: re-enable

        setCurrentSeekBarActions(curr, aim)
        setIncreaseSeekBarActions(inc)

        justSmokedButton.setOnClickListener { insertSmokeEntry(curr < aim) }
        loadLastEvents(curr)
    }

    private fun setCurrentSeekBarActions(curr: Int, aim: Int) {
        val currTimeoutValueString = currTimeoutTextBox.text.toString()
        val currTimeoutValueNumber =
            if (currTimeoutValueString.isBlank()) curr else Integer.valueOf(currTimeoutValueString)

        currTimeoutTextBox.setText(currTimeoutValueNumber.toString())
        currTimeoutSeekBar.progress = currTimeoutValueNumber
        currTimeoutSeekBar.max = aim
        currTimeoutSeekBar.isEnabled = false // TODO: re-enable

        currTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currTimeoutTextBox.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt(getString(R.string.current_timeout_key), seekBar?.progress!!)
                    apply()
                }
            }
        })
    }

    private fun setIncreaseSeekBarActions(inc: Int) {
        val increaseTimeoutValueString = increaseTimeoutTextBox.text.toString()
        val increaseTimeoutValueNumber =
            if (increaseTimeoutValueString.isBlank()) inc else Integer.valueOf(increaseTimeoutValueString)

        increaseTimeoutTextBox.setText(increaseTimeoutValueNumber.toString())
        increaseTimeoutSeekBar.progress = increaseTimeoutValueNumber
        increaseTimeoutSeekBar.max = 5

        increaseTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                increaseTimeoutTextBox.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt(getString(R.string.increase_timeout_key), seekBar?.progress!!)
                    apply()
                }
            }
        })
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
                    historyArray.push(sdf.format(Date(date)) + " - " + realTimeout + " (" + timeout + ") " + minString)
                } else {
                    historyArray.push(sdf.format(Date(date)))
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

//        justSmokedButton.isEnabled = true
    }

    private fun insertSmokeEntry(needInc: Boolean) {
        val currTime = System.currentTimeMillis()

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val lastSmokeTime = sharedPref.getLong(
            getString(R.string.last_smoke_time_key),
            0
        )

        val currTimeout = sharedPref.getInt(
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
        logger.log(Level.INFO, "Row ID: $newRowId")

        with(sharedPref.edit()) {
            putLong(getString(R.string.last_smoke_time_key), currTime)
            apply()
        }

        if (needInc && lastSmokeTime > 0) {
            if (DateUtils.toDays(lastSmokeTime) > DateUtils.toDays(currTime)) increaseTimeout(sharedPref)
        }

        loadLastEvents(currTimeout)
    }

    private fun increaseTimeout(sharedPref: SharedPreferences) {
        val increasedTimeout = sharedPref.getInt(
            getString(R.string.current_timeout_key),
            resources.getInteger(R.integer.current_timeout_default_key)
        ) + 1

        with(sharedPref.edit()) {
            putInt(getString(R.string.current_timeout_key), increasedTimeout)
            apply()
        }

        currTimeoutSeekBar.progress = increasedTimeout
        currTimeoutTextBox.setText(increasedTimeout.toString())
    }
}