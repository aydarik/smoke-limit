package ru.gumerbaev.smokelimit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ru.gumerbaev.smokelimit.R
import ru.gumerbaev.smokelimit.entity.SmokeEntity

class SmokeAdapter(private val entries: List<SmokeEntity>, private val context: Context) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater;
    private var avg: Long = 0;

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.entry, parent, false)

        val entry = getItem(position)

        val delayText = view.findViewById(R.id.entryDelay) as TextView
        delayText.text = entry.getDelayString()

        val delay = entry.getDelay()
        if (delay != null && delay < avg) delayText.setTextColor(context.getColor(R.color.colorAccent))

        val dateText = view.findViewById(R.id.entryDate) as TextView
        dateText.text = entry.getDateString()

        return view
    }

    override fun getItem(position: Int): SmokeEntity {
        return entries[position]
    }

    override fun getItemId(position: Int): Long {
        return entries[position].id ?: 0
    }

    override fun getCount(): Int {
        return entries.size
    }

    override fun notifyDataSetChanged() {
        avg = median()
        super.notifyDataSetChanged()
    }

    private fun median(): Long {
        if (entries.size < 2) return 0
        val sortedDelays = entries.stream().mapToLong { it.getDelay() ?: 0 }.sorted()
        return if (entries.size % 2 == 0)
            sortedDelays.skip((entries.size / 2 - 1).toLong()).limit(2).average().orElse(0.0).toLong()
        else
            sortedDelays.skip((entries.size / 2).toLong()).findFirst().orElse(0)
    }

    fun average(): Long? {
        return avg
    }
}