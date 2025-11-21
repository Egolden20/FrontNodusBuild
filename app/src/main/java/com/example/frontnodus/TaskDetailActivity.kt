package com.example.frontnodus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.frontnodus.adapter.TaskDetailPagerAdapter
import com.example.frontnodus.databinding.ActivityTaskDetailBinding
import com.google.android.material.tabs.TabLayoutMediator

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get task data from intent
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Tarea"
        val taskStatus = intent.getStringExtra("TASK_STATUS") ?: "Pendiente"

        // Set task info
        binding.tvTaskTitle.text = taskTitle
        binding.tvTaskStatus.text = taskStatus

        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Setup ViewPager with TabLayout
        setupViewPagerWithTabs()
    }

    private fun setupViewPagerWithTabs() {
        val adapter = TaskDetailPagerAdapter(this)
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
