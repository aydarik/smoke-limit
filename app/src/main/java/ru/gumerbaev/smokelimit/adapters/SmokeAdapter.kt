package ru.gumerbaev.smokelimit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ru.gumerbaev.smokelimit.R
import ru.gumerbaev.smokelimit.entity.SmokeEntity
import ru.gumerbaev.smokelimit.utils.DateUtils

class SmokeAdapter(context: Context) : BaseAdapter() {

//    companion object {
//        const val HOUR = 60 * 60 * 1000
//    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var entries: List<SmokeEntity>? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.entry, parent, false)

        val entry = getItem(position)
        val delayMs = entry?.getDelayMs()

        val delayText = view.findViewById(R.id.entryDelay) as TextView
        delayText.text = if (delayMs != null) DateUtils.delayString(delayMs) else ""
//        if (delayMs != null && delayMs < HOUR) delayText.alpha = 0.7f

        val dateText = view.findViewById(R.id.entryDate) as TextView
        dateText.text = entry?.getDateString() ?: ""

        return view
    }

    fun setEntries(entries: List<SmokeEntity>) {
        this.entries = entries
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): SmokeEntity? {
        return entries?.get(position)
    }

    override fun getItemId(position: Int): Long {
        return entries?.get(position)?.id ?: 0
    }

    override fun getCount(): Int {
        return entries?.size ?: 0
    }
}