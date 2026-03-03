package com.example.frontnodus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.domain.models.FileItem

class TaskFileAdapter(
    private val onFileClick: (FileItem) -> Unit,
    private val onDownloadClick: (FileItem) -> Unit
) : ListAdapter<FileItem, TaskFileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        holder.bind(file, onFileClick, onDownloadClick)
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFileTitle: TextView = itemView.findViewById(R.id.tvFileTitle)
        private val tvFileDescription: TextView = itemView.findViewById(R.id.tvFileDescription)
        private val tvFileSource: TextView = itemView.findViewById(R.id.tvFileSource)
        private val tvFileDate: TextView = itemView.findViewById(R.id.tvFileDate)
        private val btnDownload: Button = itemView.findViewById(R.id.btnDownload)

        fun bind(file: FileItem, onClick: (FileItem) -> Unit, onDownloadClick: (FileItem) -> Unit) {
            tvFileTitle.text = file.title
            tvFileDescription.text = file.description?.takeIf { it.isNotEmpty() } ?: "Sin descripción"
            tvFileSource.text = file.source
            tvFileDate.text = file.uploadDate

            itemView.setOnClickListener {
                onClick(file)
            }

            btnDownload.setOnClickListener {
                onDownloadClick(file)
            }
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
