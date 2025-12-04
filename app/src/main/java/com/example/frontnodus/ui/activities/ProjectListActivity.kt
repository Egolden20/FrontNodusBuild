package com.example.frontnodus.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R
import com.example.frontnodus.ui.adapters.ProjectAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ProjectListActivity : AppCompatActivity() {

    private val projectRepository: com.example.frontnodus.data.repository.ProjectRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_list)

        // Back button
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvProjectList)
        rv.layoutManager = LinearLayoutManager(this)

        // Start with cached projects then refresh
        lifecycleScope.launch {
            val cached = try { projectRepository.getCachedProjects() } catch (e: Exception) { emptyList() }
            var adapter = ProjectAdapter(cached, onProjectClick = { p ->
                val intent = Intent(this@ProjectListActivity, ProjectDetailActivity::class.java)
                intent.putExtra("projectId", p.id)
                intent.putExtra("projectTitle", p.title)
                intent.putExtra("projectRole", p.role)
                startActivity(intent)
            }, onViewAllClick = { p ->
                val intent = Intent(this@ProjectListActivity, ProjectTasksActivity::class.java)
                intent.putExtra("projectId", p.id)
                intent.putExtra("projectTitle", p.title)
                startActivity(intent)
            })
            rv.adapter = adapter

            val remote = try { projectRepository.fetchAndCacheProjects() } catch (e: Exception) { emptyList() }
            // always update adapter with remote results (may be empty) so UI matches server
            adapter = ProjectAdapter(remote, onProjectClick = { p ->
                val intent = Intent(this@ProjectListActivity, ProjectDetailActivity::class.java)
                intent.putExtra("projectId", p.id)
                intent.putExtra("projectTitle", p.title)
                intent.putExtra("projectRole", p.role)
                startActivity(intent)
            }, onViewAllClick = { p ->
                val intent = Intent(this@ProjectListActivity, ProjectTasksActivity::class.java)
                intent.putExtra("projectId", p.id)
                intent.putExtra("projectTitle", p.title)
                startActivity(intent)
            })
            rv.adapter = adapter
        }
    }
}
