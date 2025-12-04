package com.example.frontnodus.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.ui.adapters.TaskAdapter
import com.example.frontnodus.data.repository.TaskRepository
import com.example.frontnodus.data.repository.ProjectRepository
import android.content.Intent
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ProjectDetailActivity : AppCompatActivity() {

    private val taskRepository: TaskRepository by inject()
    private val projectRepository: ProjectRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        val projectId = intent.getStringExtra("projectId") ?: ""
        val projectTitle = intent.getStringExtra("projectTitle") ?: "Proyecto"
        val projectRole = intent.getStringExtra("projectRole") ?: "-"

        // Header title (will be overwritten after fetching)
        val tvName = findViewById<android.widget.TextView>(R.id.tvProjectName)
        val tvRole = findViewById<android.widget.TextView>(R.id.tvProjectRoleDetail)
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvProjectStatus)
        val tvOwner = findViewById<android.widget.TextView>(R.id.tvProjectOwner)
        val tvDates = findViewById<android.widget.TextView>(R.id.tvProjectDates)
        val tvDescription = findViewById<android.widget.EditText>(R.id.tvProjectDescription)
        val tvTeam = findViewById<android.widget.TextView>(R.id.tvProjectTeam)
        val tvLocation = findViewById<android.widget.TextView>(R.id.tvProjectLocation)

        tvName.text = projectTitle
        tvRole.text = projectRole

        // Back button
        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvDetailTasks)
        rv.layoutManager = LinearLayoutManager(this)

        // Load tasks for the project from backend
        lifecycleScope.launch {
            try {
                // Load project details
                val detail = try {
                    projectRepository.fetchProjectById(projectId)
                } catch (e: Exception) {
                    null
                }

                if (detail != null) {
                    tvName.text = detail.name ?: tvName.text
                    tvStatus.text = detail.status ?: tvStatus.text
                    tvOwner.text = detail.ownerEmail ?: tvOwner.text
                    tvDates.text = listOfNotNull(detail.startDate, detail.endDate).joinToString(" - ")
                    tvDescription.setText(detail.description ?: "")
                    tvTeam.text = if (detail.team.isNotEmpty()) detail.team.joinToString(", ") { m ->
                        val who = m.name ?: m.email ?: m.id ?: "Usuario"
                        if (!m.role.isNullOrBlank()) "$who (${m.role})" else who
                    } else "-"
                    tvLocation.text = detail.locationAddress ?: "-"
                }

                val tasks = taskRepository.getTasksByProject(projectId)
                rv.adapter = TaskAdapter(tasks) { task ->
                    val intent = Intent(this@ProjectDetailActivity, TaskDetailActivity::class.java).apply {
                        putExtra("TASK_ID", task.id)
                        putExtra("TASK_TITLE", task.title)
                        putExtra("TASK_STATUS", task.status)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // fallback to empty list on error
                rv.adapter = TaskAdapter(emptyList()) { }
            }
        }
    }
}
