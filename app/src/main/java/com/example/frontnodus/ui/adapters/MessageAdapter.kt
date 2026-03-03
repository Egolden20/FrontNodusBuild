package com.example.frontnodus.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val meId: String?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<Message>()
    private val TAG = "MessageAdapter"

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun getItemViewType(position: Int): Int {
        val msg = items[position]
        val isSent = msg.from != null && msg.from == meId
        Log.d(TAG, "getItemViewType: position=$position, msg.from=${msg.from}, meId=$meId, isSent=$isSent")
        return if (isSent) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
                SentViewHolder(v)
            }
            VIEW_TYPE_RECEIVED -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
                ReceivedViewHolder(v)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        val formattedTime = formatTime(msg.createdAt)
        
        when (holder) {
            is SentViewHolder -> {
                holder.tvMessage.text = msg.text
                holder.tvTime.text = formattedTime
            }
            is ReceivedViewHolder -> {
                holder.tvMessage.text = msg.text
                holder.tvTime.text = formattedTime
            }
        }
    }

    private fun formatTime(timestamp: String?): String {
        if (timestamp.isNullOrEmpty() || timestamp == "now") {
            val now = Calendar.getInstance()
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        }
        
        return try {
            // Try parsing as timestamp in milliseconds first
            val time = timestamp.toLongOrNull()
            if (time != null) {
                return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
            }
            
            // Try parsing ISO timestamp from backend
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timestamp)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            // If all parsing fails, show the timestamp as-is or current time
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    fun addMessage(msg: Message) {
        Log.d(TAG, "addMessage called: id=${msg.id}, tempId=${msg.tempId}, text=${msg.text}, from=${msg.from}")
        Log.d(TAG, "current items count: ${items.size}")
        
        // avoid duplicates by id or tempId
        val existsById = items.any { it.id != null && it.id == msg.id }
        val existsByTempId = items.any { it.tempId != null && msg.tempId != null && it.tempId == msg.tempId }
        
        if (existsById) {
            Log.w(TAG, "Message already exists by id: ${msg.id}")
            return
        }
        if (existsByTempId) {
            Log.w(TAG, "Message already exists by tempId: ${msg.tempId}")
            return
        }
        
        items.add(msg)
        Log.d(TAG, "Message added! New count: ${items.size}, position: ${items.size - 1}")
        notifyItemInserted(items.size - 1)
    }

    fun updateMessageByTempId(tempId: String, newId: String) {
        Log.d(TAG, "updateMessageByTempId: tempId=$tempId, newId=$newId")
        val idx = items.indexOfFirst { it.tempId == tempId }
        if (idx >= 0) {
            val old = items[idx]
            items[idx] = old.copy(id = newId)
            Log.d(TAG, "Message updated at index $idx")
            notifyItemChanged(idx)
        } else {
            Log.w(TAG, "Message with tempId=$tempId not found for update")
        }
    }
}
