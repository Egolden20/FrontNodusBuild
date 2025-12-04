package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.ui.adapters.TaskAdapter
import com.example.frontnodus.data.repository.TaskRepository
import com.example.frontnodus.domain.models.Task
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.storage.TokenStore
import com.example.frontnodus.data.repository.AuthRepository
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val authRepository: AuthRepository by inject()
    private val taskRepository: TaskRepository by inject()
    private val projectRepository: com.example.frontnodus.data.repository.ProjectRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val tokenStore: TokenStore by inject()

        // UI elements for header
        val ivUser = findViewById<ImageView>(R.id.ivUser)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvDate = findViewById<TextView>(R.id.tvDate)

        // Observe saved user name and update header
        lifecycleScope.launch {
            tokenStore.userNameFlow.collectLatest { name ->
                tvWelcome.text = "Bienvenido ${name ?: "Usuario"}"
            }
        }

        // Show current date (e.g., "15 de octubre")
        try {
            val sdf = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))
            val dateText = sdf.format(Date())
            tvDate.text = "Gestión: $dateText"
        } catch (e: Exception) {
            // fallback
            tvDate.text = "Gestión"
        }

        // Fetch current user from backend (me) and update header with firstName/lastName
        lifecycleScope.launch {
            try {
                // diagnostic: check token value
                val currentToken = tokenStore.tokenFlow.firstOrNull()
                Log.d("HomeActivity", "currentToken length=${currentToken?.length ?: 0}")

                val me: JSONObject? = authRepository.getMe()
                Log.d("HomeActivity", "me response=${me?.toString() ?: "null"}")
                if (me != null) {
                    val profile = me.optJSONObject("profile")
                    val first = profile?.optString("firstName", "") ?: ""
                    val last = profile?.optString("lastName", "") ?: ""
                    val displayName = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
                        me.optString("email", "Usuario")
                    }
                    tvWelcome.text = "Bienvenido $displayName"
                    try {
                        tokenStore.saveUserName(displayName)
                    } catch (_: Exception) { }
                }
            } catch (e: Exception) {
                // ignore - keep existing name if any
            }
        }

        // User icon -> popup menu
        ivUser.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_home_user, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_profile -> {
                        // Open profile if available
                        try {
                            val intent = Intent(this@HomeActivity, ProfileActivity::class.java)
                            startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(this@HomeActivity, "Perfil no disponible", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.menu_scanner -> {
                        try {
                            val intent = Intent(this@HomeActivity, ScannerActivity::class.java)
                            startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(this@HomeActivity, "Scanner no disponible", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.menu_logout -> {
                        lifecycleScope.launch {
                            try {
                                tokenStore.clearToken()
                                tokenStore.clearUserName()
                            } catch (_: Exception) {}
                            val intent = Intent(this@HomeActivity, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Initialize views
        val cardRegisterAdvance = findViewById<CardView>(R.id.cardRegisterAdvance)
        val cardRegisterIncident = findViewById<CardView>(R.id.cardRegisterIncident)
        val cardSchedule = findViewById<CardView>(R.id.cardSchedule)
        val cardChat = findViewById<CardView>(R.id.cardChat)

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

        cardChat.setOnClickListener {
            try {
                val intent = Intent(this, ChatListActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Mensajería no disponible", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(this)

        // Fetch tasks from backend and populate RecyclerView
        lifecycleScope.launch {
            try {
                // TODO: replace with selected project id; using empty or default id for now
                val projectId = "default"
                val tasks = try {
                    taskRepository.getTasksByProject(projectId)
                } catch (e: Exception) {
                    emptyList<com.example.frontnodus.domain.models.Task>()
                }

                taskAdapter = TaskAdapter(tasks) { task ->
                    val intent = Intent(this@HomeActivity, TaskDetailActivity::class.java).apply {
                        putExtra("TASK_TITLE", task.title)
                        putExtra("TASK_STATUS", task.status)
                    }
                    startActivity(intent)
                }

                rvTasks.adapter = taskAdapter
            } catch (e: Exception) {
                // fallback: keep empty list
                taskAdapter = TaskAdapter(emptyList()) { }
                rvTasks.adapter = taskAdapter
            }
        }

        // Projects vertical list (load from repository + cache)
        val rvProjects = findViewById<RecyclerView>(R.id.rvProjects)
        rvProjects.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        // Adapter initially empty; will be updated when data arrives
        var projAdapter = com.example.frontnodus.ui.adapters.ProjectAdapter(emptyList(), onProjectClick = { p ->
            val intent = Intent(this@HomeActivity, com.example.frontnodus.ui.activities.ProjectDetailActivity::class.java)
            intent.putExtra("projectId", p.id)
            intent.putExtra("projectTitle", p.title)
            intent.putExtra("projectRole", p.role)
            startActivity(intent)
        }, onViewAllClick = { p ->
            val intent = Intent(this@HomeActivity, ProjectListActivity::class.java)
            startActivity(intent)
        })

        rvProjects.adapter = projAdapter

        // Load cached projects first, then refresh from network
        lifecycleScope.launch {
            try {
                val cached = try { projectRepository.getCachedProjects() } catch (e: Exception) { emptyList<com.example.frontnodus.ui.adapters.ProjectCard>() }
                // always update UI with cached first (may be empty)
                projAdapter = com.example.frontnodus.ui.adapters.ProjectAdapter(cached, onProjectClick = { p ->
                    val intent = Intent(this@HomeActivity, com.example.frontnodus.ui.activities.ProjectDetailActivity::class.java)
                    intent.putExtra("projectId", p.id)
                    intent.putExtra("projectTitle", p.title)
                    intent.putExtra("projectRole", p.role)
                    startActivity(intent)
                }, onViewAllClick = { p ->
                    val intent = Intent(this@HomeActivity, ProjectListActivity::class.java)
                    startActivity(intent)
                })
                rvProjects.adapter = projAdapter

                // fetch remote and always update UI with the remote list (even if empty)
                val remote = try { projectRepository.fetchAndCacheProjects() } catch (e: Exception) { emptyList<com.example.frontnodus.ui.adapters.ProjectCard>() }
                projAdapter = com.example.frontnodus.ui.adapters.ProjectAdapter(remote, onProjectClick = { p ->
                    val intent = Intent(this@HomeActivity, com.example.frontnodus.ui.activities.ProjectDetailActivity::class.java)
                    intent.putExtra("projectId", p.id)
                    intent.putExtra("projectTitle", p.title)
                    intent.putExtra("projectRole", p.role)
                    startActivity(intent)
                }, onViewAllClick = { p ->
                    val intent = Intent(this@HomeActivity, ProjectListActivity::class.java)
                    startActivity(intent)
                })
                rvProjects.adapter = projAdapter
            } catch (e: Exception) {
                // keep empty adapter on error
            }
        }
        // 'Ver todas' header click -> show full project list
        val tvViewAll = findViewById<android.widget.TextView>(R.id.tvViewAllProjects)
        tvViewAll.setOnClickListener {
            val intent = Intent(this@HomeActivity, ProjectListActivity::class.java)
            startActivity(intent)
        }
    }
}
