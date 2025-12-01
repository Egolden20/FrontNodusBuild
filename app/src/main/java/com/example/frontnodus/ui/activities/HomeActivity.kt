package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.ui.adapters.TaskAdapter
import com.example.frontnodus.domain.models.Task

class HomeActivity : AppCompatActivity() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        val cardRegisterAdvance = findViewById<CardView>(R.id.cardRegisterAdvance)
        val cardRegisterIncident = findViewById<CardView>(R.id.cardRegisterIncident)
        val cardSchedule = findViewById<CardView>(R.id.cardSchedule)
        val cardMore = findViewById<CardView>(R.id.cardMore)

        // Set click listeners for action buttons
        cardRegisterAdvance.setOnClickListener {
            val intent = Intent(this, RegisterAdvanceActivity::class.java)
            startActivity(intent)
        }

        cardRegisterIncident.setOnClickListener {
            val intent = Intent(this, ReportIncidentActivity::class.java)
            startActivity(intent)
        }

        cardSchedule.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
        }

        cardMore.setOnClickListener {
            Toast.makeText(this, "Más opciones", Toast.LENGTH_SHORT).show()
        }

        // Setup RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(this)

        // Create static task data
        val tasks = listOf(
            Task(
                id = 1,
                title = "Inspección de cimentación Bloque A",
                subtitle = "Realizado por: Las Terrazas",
                date = "2025-10-25",
                status = "Nuevo",
                actionButton = "Gestionar"
            ),
            Task(
                id = 2,
                title = "Verificar instalaciones eléctricas",
                subtitle = "Torre Residencial Sur",
                date = "2025-10-22",
                status = "Nuevo",
                actionButton = "Atendido"
            )
        )

        // Initialize adapter
        taskAdapter = TaskAdapter(tasks) { task ->
            val intent = Intent(this, TaskDetailActivity::class.java).apply {
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_STATUS", task.status)
            }
            startActivity(intent)
        }

        rvTasks.adapter = taskAdapter
    }
}
