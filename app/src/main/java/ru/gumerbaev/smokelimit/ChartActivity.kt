package ru.gumerbaev.smokelimit

import android.os.Bundle
import android.util.Log
import android.util.LongSparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.keyIterator
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.activity_chart.*
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.data.SmokesDbQueryExecutor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ChartActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ChartActivity"
        const val INCREMENTER = 24 * 60 * 60 * 1000
    }

    private val _dbExecutor = SmokesDbQueryExecutor(SmokesDbHelper(this))
    private val _tzOffset = TimeZone.getDefault().rawOffset

    internal inner class DateAxisValueFormatter : ValueFormatter() {
        private val _sdf = SimpleDateFormat("MM/dd", Locale.getDefault())

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return _sdf.format(Date(value.toLong() * INCREMENTER + _tzOffset))
        }
    }

    internal inner class IntValueFormatter : ValueFormatter() {

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return value.toInt().toString()
        }

        override fun getBarLabel(barEntry: BarEntry?): String {
            return barEntry?.y?.toInt().toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        Log.d(TAG, "Chart activity created")

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = DateAxisValueFormatter()

        val intFormatter = IntValueFormatter()
        chart.axisLeft.valueFormatter = intFormatter
        chart.axisRight.valueFormatter = intFormatter
        chart.axisRight.isEnabled = false

        val map = LongSparseArray<Int>()
        val entries = _dbExecutor.getLastEntries(1000)
        entries.forEach {
            val key = (it.date.time + _tzOffset) / INCREMENTER
            val currVal = map[key] ?: 0
            map.put(key, currVal + 1)
        }

        val list = ArrayList<BarEntry>()
        for (k in map.keyIterator()) {
            list.add(
                BarEntry(k.toFloat(), map[k].toFloat())
            )
        }

        val dataSet = BarDataSet(list, getString(R.string.amount))
        dataSet.valueFormatter = intFormatter
        chart.data = BarData(dataSet)

        chart.invalidate() // refresh
    }
}
