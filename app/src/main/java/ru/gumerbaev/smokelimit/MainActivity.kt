package ru.gumerbaev.smokelimit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.gumerbaev.smokelimit.adapters.SmokeAdapter
import ru.gumerbaev.smokelimit.entity.SmokeEntity
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.data.SmokesDbQueryExecutor
import ru.gumerbaev.smokelimit.utils.DateUtils
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val _tag = "MyActivity"
    private val _dbExecutor = SmokesDbQueryExecutor(SmokesDbHelper(this))

    private var _currTimeout: Int? = null
    private var _incTimeout: Int? = null
    private var _lastEntry: SmokeEntity? = null

    private val _smokeEntries = ArrayList<SmokeEntity>()
    private var _smokeAdapter: SmokeAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsLayout.visibility = View.GONE

        _smokeAdapter = SmokeAdapter(_smokeEntries, this)
        historyList.adapter = _smokeAdapter

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        _currTimeout = sharedPref.getInt(
            getString(R.string.current_timeout_key),
            resources.getInteger(R.integer.current_timeout_default_key)
        )
        _incTimeout = sharedPref.getInt(
            getString(R.string.increase_timeout_key),
            resources.getInteger(R.integer.increase_timeout_default_key)
        )

        currTimeoutTextBox.value = _currTimeout!!
        currTimeoutTextBox.setOnValueChangedListener { _, _, newVal ->
            _currTimeout = newVal
            with(sharedPref.edit()) {
                putInt(getString(R.string.current_timeout_key), newVal)
                apply()
            }
        }

        increaseTimeoutTextBox.value = _incTimeout!!
        increaseTimeoutTextBox.setOnValueChangedListener { _, _, newVal ->
            _incTimeout = newVal
            with(sharedPref.edit()) {
                putInt(getString(R.string.increase_timeout_key), newVal)
                apply()
            }
        }

        justSmokedButton.setOnClickListener { insertSmokeEntry() }
        lockButton.setOnClickListener {
            if (settingsLayout.visibility != View.GONE) {
                settingsLayout.visibility = View.GONE
                lockButton.setIconResource(android.R.drawable.arrow_up_float)
            } else {
                settingsLayout.visibility = View.VISIBLE
                lockButton.setIconResource(android.R.drawable.arrow_down_float)
            }
        }
        chartButton.setOnClickListener {
            val intent = Intent(this, ChartActivity::class.java).apply {}
            startActivity(intent)
        }
        loadLastEvents()
    }

    @SuppressLint("SetTextI18n")
    private fun loadLastEvents() {
        val limit = 200

        val entries = _dbExecutor.getEntries(limit + 1)
        if (entries.size == limit) entries.dropLast(1)

        with(_smokeEntries) {
            clear()
            addAll(entries)
        }
        _smokeAdapter?.notifyDataSetChanged()

        _lastEntry = entries.firstOrNull()

        if (_lastEntry != null) {
            val realTimeoutMs = System.currentTimeMillis() - _lastEntry!!.date.time
            val accessible = DateUtils.toMinutes(realTimeoutMs) > _currTimeout!!

            if (accessible) justSmokedButton.text = getString(R.string.just_smoked)
            else justSmokedButton.text = DateUtils.remainString(realTimeoutMs, _currTimeout!!)

            justSmokedButton.isEnabled = accessible
        } else justSmokedButton.isEnabled = true
    }

    private fun insertSmokeEntry() {
        if (_lastEntry != null) {
            val dateDiff = DateUtils.dayDiff(_lastEntry!!.date)
            if (dateDiff > 0) {
                increaseTimeout(dateDiff)
            }
        }

        _dbExecutor.addEntry(SmokeEntity(Date(), _currTimeout!!))
        loadLastEvents()
    }

    private fun increaseTimeout(incDays: Int) {
        val maxTimeout = resources.getInteger(R.integer.max_timeout_default_key)
        _currTimeout = _currTimeout?.plus((_incTimeout!! * incDays))
        if (_currTimeout!! > maxTimeout) {
            _currTimeout = maxTimeout
            Log.d(_tag, "Max value reached")
        }

        with(getPreferences(Context.MODE_PRIVATE).edit()) {
            putInt(getString(R.string.current_timeout_key), _currTimeout!!)
            apply()
        }

        currTimeoutTextBox.value = _currTimeout!!
    }
}