package ru.gumerbaev.smokelimit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_chart.*
import com.github.mikephil.charting.data.LineData
import ru.gumerbaev.smokelimit.data.SmokesDbHelper
import ru.gumerbaev.smokelimit.data.SmokesDbQueryExecutor
import ru.gumerbaev.smokelimit.utils.DateUtils

class ChartActivity : AppCompatActivity() {

    private val _dbExecutor = SmokesDbQueryExecutor(SmokesDbHelper(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        val list = _dbExecutor.getEntries(1000)
        val firstTime = list.lastOrNull()?.date?.time
        val entries = list.map { e ->
            Entry(DateUtils.toMinutes(e.date.time - firstTime!!).toFloat(),
                e.getDelay()?.let { DateUtils.toMinutes(it).toFloat() } ?: 0f)
        }
        val dataSet = LineDataSet(entries, getString(R.string.delay));
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate() // Refresh
    }
}
