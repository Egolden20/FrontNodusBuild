package com.example.frontnodus.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.domain.models.ChecklistItem

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val onItemCheckedChange: (ChecklistItem, Boolean) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChecklistItem: TextView = itemView.findViewById(R.id.tvChecklistItem)
        val cbChecklistItem: CheckBox = itemView.findViewById(R.id.cbChecklistItem)

        fun bind(item: ChecklistItem) {
            tvChecklistItem.text = item.text
            cbChecklistItem.isChecked = item.isChecked
            
            // Apply strikethrough based on checked state
            updateStrikethrough(item.isChecked)

            cbChecklistItem.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                updateStrikethrough(isChecked)
                onItemCheckedChange(item, isChecked)
            }

            itemView.setOnClickListener {
                cbChecklistItem.isChecked = !cbChecklistItem.isChecked
            }
        }
        
        private fun updateStrikethrough(isChecked: Boolean) {
            if (isChecked) {
                tvChecklistItem.paintFlags = tvChecklistItem.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvChecklistItem.paintFlags = tvChecklistItem.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
