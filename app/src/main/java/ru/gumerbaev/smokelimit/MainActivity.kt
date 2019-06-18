package ru.gumerbaev.smokelimit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.gumerbaev.smokelimit.adapters.SmokeAdapter
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.data.SmokesDbQueryExecutor
import ru.gumerbaev.smokelimit.entity.SmokeEntity
import ru.gumerbaev.smokelimit.service.TimerNotificationService
import ru.gumerbaev.smokelimit.utils.DateUtils
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val _dbExecutor = SmokesDbQueryExecutor(SmokesDbHelper(this))

    private var _currTimeout: Int? = null
    private var _incTimeout: Int? = null
    private var _lastEntry: SmokeEntity? = null

    private var _smokeAdapter: SmokeAdapter? = null

    private var _timeBinder: TimerNotificationService.TimeBinder? = null
    private val _serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            _timeBinder = service as TimerNotificationService.TimeBinder
            checkState()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            _timeBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val timerIntent = TimerNotificationService.getIntent(this)
        startForegroundService(timerIntent)

        settingsLayout.visibility = View.GONE

        _smokeAdapter = SmokeAdapter(this)
        historyList.adapter = _smokeAdapter
        loadLastEvents()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
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

        justSmokedButton.setOnClickListener {
            justSmokedButton.isEnabled = false
            insertSmokeEntry()
        }

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

        fixedRateTimer(
            "time_check", true, Date(), 30 * 1000
        ) {
            checkState()
        }
    }

    override fun onResume() {
        val timerIntent = TimerNotificationService.getIntent(this)
        bindService(timerIntent, _serviceConnection, Context.BIND_AUTO_CREATE)
        super.onResume()
    }

    override fun onStop() {
        unbindService(_serviceConnection)
        super.onStop()
    }

    private fun loadLastEvents() {
        val entries = _dbExecutor.getLastEntries(200)
        _smokeAdapter?.setEntries(entries)
        _lastEntry = entries.firstOrNull()
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
        checkState()

        _timeBinder?.update()
    }

    private fun increaseTimeout(incDays: Int) {
        val maxTimeout = resources.getInteger(R.integer.max_timeout_default_key)
        _currTimeout = _currTimeout?.plus((_incTimeout!! * incDays)) ?: return
        if (_currTimeout!! > maxTimeout) {
            _currTimeout = maxTimeout
            Log.d(TAG, "Max value reached")
        }

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putInt(getString(R.string.current_timeout_key), _currTimeout!!)
            apply()
        }

        currTimeoutTextBox.value = _currTimeout!!
    }

    private fun checkState() {
        val remain = _timeBinder?.getRemain(_lastEntry)
        runOnUiThread {
            justSmokedButton.text = DateUtils.minString(remain ?: 0)
            justSmokedButton.isEnabled = remain != null && remain >= 0

//                justSmokedButton.setBackgroundColor(
//                    if (realTimeoutMs != null && realTimeoutMs < _smokeAdapter?.average() ?: 0) getColor(R.color.colorAccent)
//                    else getColor(R.color.colorPrimary)
//                )
        }
    }
}