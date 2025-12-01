package com.example.frontnodus.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.frontnodus.ui.fragments.ChecklistFragment
import com.example.frontnodus.ui.fragments.CommentsFragment
import com.example.frontnodus.ui.fragments.FilesFragment

class TaskDetailPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChecklistFragment()
            1 -> FilesFragment()
            2 -> CommentsFragment()
            else -> ChecklistFragment()
        }
    }
}
