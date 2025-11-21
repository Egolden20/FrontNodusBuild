package com.example.frontnodus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.model.Event

class EventAdapter(
    private val events: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventDay: TextView = itemView.findViewById(R.id.tvEventDay)
        val tvEventMonth: TextView = itemView.findViewById(R.id.tvEventMonth)
        val tvEventStatus: TextView = itemView.findViewById(R.id.tvEventStatus)
        val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)

        fun bind(event: Event) {
            tvEventDay.text = event.day
            tvEventMonth.text = event.monthYear
            tvEventStatus.text = event.status
            tvEventTitle.text = event.title
            tvEventLocation.text = event.location

            itemView.setOnClickListener {
                onEventClick(event)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size
}
