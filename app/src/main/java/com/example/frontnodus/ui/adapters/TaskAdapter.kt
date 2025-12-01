package com.example.frontnodus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.domain.models.Task

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskSubtitle: TextView = itemView.findViewById(R.id.tvTaskSubtitle)
        val tvTaskDate: TextView = itemView.findViewById(R.id.tvTaskDate)
        val tvTaskStatus: TextView = itemView.findViewById(R.id.tvTaskStatus)
        val btnTaskAction: Button = itemView.findViewById(R.id.btnTaskAction)

        fun bind(task: Task) {
            tvTaskTitle.text = task.title
            tvTaskSubtitle.text = task.subtitle
            tvTaskDate.text = task.date
            tvTaskStatus.text = task.status
            btnTaskAction.text = task.actionButton

            btnTaskAction.setOnClickListener {
                onTaskClick(task)
            }

            itemView.setOnClickListener {
                onTaskClick(task)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size
}
