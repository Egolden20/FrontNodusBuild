package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.frontnodus.ui.adapters.SchedulePagerAdapter
import com.example.frontnodus.databinding.ActivityScheduleBinding
import com.google.android.material.tabs.TabLayoutMediator

class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Setup ViewPager with TabLayout
        setupViewPagerWithTabs()
    }

    private fun setupViewPagerWithTabs() {
        val adapter = SchedulePagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Link TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_calendar)
                1 -> getString(R.string.tab_list)
                else -> ""
            }
        }.attach()
    }
}
