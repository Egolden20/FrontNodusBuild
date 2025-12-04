package com.example.frontnodus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.models.Message

class MessageAdapter(private val meId: String?) : RecyclerView.Adapter<MessageAdapter.VH>() {
    private val items = mutableListOf<Message>()

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvMessageText)
        val tvTime: TextView = itemView.findViewById(R.id.tvMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        holder.tvText.text = msg.text
        holder.tvTime.text = msg.createdAt ?: ""
        // simple alignment: if from == meId, align end, else start
        val params = holder.tvText.layoutParams as ViewGroup.MarginLayoutParams
        if (msg.from != null && msg.from == meId) {
            holder.tvText.setBackgroundResource(android.R.color.holo_blue_dark)
            params.marginStart = 80
            params.marginEnd = 0
        } else {
            holder.tvText.setBackgroundResource(android.R.color.darker_gray)
            params.marginStart = 0
            params.marginEnd = 80
        }
        holder.tvText.layoutParams = params
    }

    fun addMessage(msg: Message) {
        // avoid duplicates by id or tempId
        val exists = items.any { (it.id != null && it.id == msg.id) || (it.tempId != null && it.tempId == msg.tempId) }
        if (exists) return
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    fun updateMessageByTempId(tempId: String, newId: String) {
        val idx = items.indexOfFirst { it.tempId == tempId }
        if (idx >= 0) {
            val old = items[idx]
            items[idx] = old.copy(id = newId)
            notifyItemChanged(idx)
        }
    }
}
