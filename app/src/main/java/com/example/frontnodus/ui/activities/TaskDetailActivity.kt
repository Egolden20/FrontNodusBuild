package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.frontnodus.ui.adapters.TaskDetailPagerAdapter
import com.example.frontnodus.databinding.ActivityTaskDetailBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.repository.TaskRepository
import com.example.frontnodus.utils.DateUtils

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private val taskRepository: TaskRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.ivBack.setOnClickListener { finish() }

        // ViewPager will be setup after we know taskId (so fragments receive TASK_ID)

        // Try to fetch full task using TASK_ID; fall back to intent title/status if missing
        val taskId = intent.getStringExtra("TASK_ID")
        val fallbackTitle = intent.getStringExtra("TASK_TITLE") ?: getString(R.string.task_detail)
        val fallbackStatus = intent.getStringExtra("TASK_STATUS") ?: ""

        // Start with empty fields while loading
        binding.tvTaskTitle.text = ""
        binding.tvTaskStatus.text = ""
        binding.tvTaskDescription.text = ""
        binding.tvDeadline.text = ""
        binding.tvAssigned.text = ""
        binding.tvProject.text = ""

        if (taskId.isNullOrBlank()) {
            // no id provided: show fallbacks
            binding.tvTaskTitle.text = fallbackTitle
            binding.tvTaskStatus.text = fallbackStatus
            // still setup pager without id
            setupViewPagerWithTabs(null)
        } else {
            lifecycleScope.launch {
                try {
                    val taskJson = taskRepository.getTaskById(taskId)
                    if (taskJson != null) {
                        val title = taskJson.optString("title", fallbackTitle)
                        val description = taskJson.optString("description", "")
                        val plannedRaw = taskJson.optString("plannedDate", taskJson.optString("createdAt", ""))
                        val planned = DateUtils.formatToDate(plannedRaw) ?: ""
                        val status = taskJson.optString("status", fallbackStatus)
                        val assignedObj = taskJson.optJSONObject("assignedTo")?.optJSONObject("profile")
                        val assignedName = assignedObj?.optString("firstName")?.plus(" ")?.plus(assignedObj.optString("lastName")) ?: "-"
                        val projectName = taskJson.optJSONObject("project")?.optString("name") ?: "-"

                        binding.tvTaskTitle.text = title
                        binding.tvTaskStatus.text = status
                        binding.tvTaskDescription.text = description
                        binding.tvDeadline.text = if (planned.isNotBlank()) planned else "-"
                        binding.tvAssigned.text = assignedName
                        binding.tvProject.text = projectName
                    } else {
                        // no data returned: show fallbacks
                        binding.tvTaskTitle.text = fallbackTitle
                        binding.tvTaskStatus.text = fallbackStatus
                    }
                } catch (e: Exception) {
                    // on error show fallbacks
                    binding.tvTaskTitle.text = fallbackTitle
                    binding.tvTaskStatus.text = fallbackStatus
                }
            }
            // after launching fetch, setup pager with taskId so fragments can also load data
            setupViewPagerWithTabs(taskId)
        }
    }

    private fun setupViewPagerWithTabs(taskId: String?) {
        val adapter = TaskDetailPagerAdapter(this, taskId)
        binding.viewPager.adapter = adapter

        // Link TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_checklist)
                1 -> getString(R.string.tab_files)
                2 -> getString(R.string.tab_comments)
                else -> ""
            }
        }.attach()
    }
}
