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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.entry, parent, false)

        val entry = getItem(position) as SmokeEntity
        (view.findViewById(R.id.entryDelay) as TextView).text = entry.getDelayString()
        (view.findViewById(R.id.entryDate) as TextView).text = entry.getDateString()

        return view
    }

    override fun getItem(position: Int): Any {
        return entries[position]
    }

    override fun getItemId(position: Int): Long {
        return entries[position].id ?: 0
    }

    override fun getCount(): Int {
        return entries.size
    }
}