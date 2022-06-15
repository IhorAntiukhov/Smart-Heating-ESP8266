package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("SetTextI18n")
@Suppress("DEPRECATION")
class BoilerRecyclerAdapter(private val timestampsArrayList : ArrayList<String>,
                            private val timestampTypesArrayList : ArrayList<Boolean>) :
        RecyclerView.Adapter<BoilerRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelTimestamp : TextView = view.findViewById(R.id.labelBoilerTimestamp)
        val cardViewTimestamp : CardView = view.findViewById(R.id.cardViewBoilerTimestamp)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.boiler_recycler_view_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.labelTimestamp.text = timestampsArrayList[position]
        if (timestampTypesArrayList[position]) {
            viewHolder.cardViewTimestamp.setCardBackgroundColor(Color.parseColor("#00D679"))
        } else {
            viewHolder.cardViewTimestamp.setCardBackgroundColor(Color.parseColor("#7CD6A3"))
        }
    }

    override fun getItemCount() = timestampsArrayList.size
}