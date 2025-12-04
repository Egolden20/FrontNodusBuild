package com.example.frontnodus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R

data class ProjectCard(
    val id: String,
    val title: String,
    val startDate: String?,
    val endDate: String?,
    val role: String?
)

class ProjectAdapter(
    private val projects: List<ProjectCard>,
    private val onProjectClick: (ProjectCard) -> Unit,
    private val onViewAllClick: (ProjectCard) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvProjectTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvProjectDate)
        val tvRole: TextView = itemView.findViewById(R.id.tvProjectRole)
        val tvViewAll: TextView = itemView.findViewById(R.id.tvProjectViewAll)

        fun bind(p: ProjectCard) {
            tvTitle.text = p.title
            // Show timeline start-end if available, otherwise show start or empty
            tvDate.text = when {
                !p.startDate.isNullOrBlank() && !p.endDate.isNullOrBlank() -> "Inicio: ${p.startDate} — Fin: ${p.endDate}"
                !p.startDate.isNullOrBlank() -> "Inicio: ${p.startDate}"
                else -> ""
            }
            tvRole.text = "Tu rol: ${p.role ?: "-"}"

            itemView.setOnClickListener { onProjectClick(p) }
            tvViewAll.setOnClickListener { onViewAllClick(p) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(v)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    override fun getItemCount(): Int = projects.size
}
