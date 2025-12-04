package com.example.frontnodus.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.data.repository.ProjectRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ChatListActivity : AppCompatActivity() {

    private val projectRepository: ProjectRepository by inject()
    private lateinit var spProjects: Spinner
    private lateinit var rvProjectUsers: RecyclerView
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        spProjects = findViewById(R.id.spProjects)
        rvProjectUsers = findViewById(R.id.rvProjectUsers)

        rvProjectUsers.layoutManager = LinearLayoutManager(this)
        usersAdapter = UsersAdapter(emptyList()) { member ->
            // Open chat with selected member
            val intent = Intent(this@ChatListActivity, ChatActivity::class.java)
            intent.putExtra("userId", member.id)
            intent.putExtra("userName", member.name ?: member.email ?: "Contacto")
            startActivity(intent)
        }
        rvProjectUsers.adapter = usersAdapter

        // Load projects into spinner
        lifecycleScope.launch {
            try {
                val projects = try { projectRepository.fetchAndCacheProjects() } catch (e: Exception) { emptyList<com.example.frontnodus.ui.adapters.ProjectCard>() }
                val titles = projects.map { it.title ?: "Proyecto" }
                val ids = projects.map { it.id }
                val adapter = ArrayAdapter(this@ChatListActivity, android.R.layout.simple_spinner_item, titles)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spProjects.adapter = adapter

                spProjects.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val projectId = ids.getOrNull(position)
                        if (projectId != null) loadProjectUsers(projectId)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatListActivity, "Error cargando proyectos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProjectUsers(projectId: String) {
        lifecycleScope.launch {
            try {
                val detail = try { projectRepository.fetchProjectById(projectId) } catch (e: Exception) { null }
                val team = detail?.team ?: emptyList()
                usersAdapter.update(team)
            } catch (e: Exception) {
                usersAdapter.update(emptyList())
            }
        }
    }

    private class UsersAdapter(
        private var items: List<ProjectRepository.TeamMember>,
        private val onClick: (ProjectRepository.TeamMember) -> Unit
    ) : RecyclerView.Adapter<UsersAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvUserName)
            val tvRole: TextView = view.findViewById(R.id.tvUserRole)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project_user, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name ?: item.email ?: "Usuario"
            holder.tvRole.text = item.role ?: "Miembro"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size

        fun update(newItems: List<ProjectRepository.TeamMember>) {
            this.items = newItems
            notifyDataSetChanged()
        }
    }
}
