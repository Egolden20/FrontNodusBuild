package com.example.frontnodus.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.ui.adapters.TaskAdapter

class ProjectTasksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_tasks)

        val projectId = intent.getStringExtra("projectId") ?: ""
        val projectTitle = intent.getStringExtra("projectTitle") ?: "Tareas"

        findViewById<android.widget.TextView>(R.id.tvProjectTasksTitle).text = projectTitle

        val rv = findViewById<RecyclerView>(R.id.rvProjectTasks)
        rv.layoutManager = LinearLayoutManager(this)

        // For design: reuse static tasks or empty list. Later connect to TaskRepository.getTasksByProject(projectId)
        val tasks = listOf<com.example.frontnodus.domain.models.Task>()
        rv.adapter = TaskAdapter(tasks) { task ->
            // open detail if needed
        }
    }
}
