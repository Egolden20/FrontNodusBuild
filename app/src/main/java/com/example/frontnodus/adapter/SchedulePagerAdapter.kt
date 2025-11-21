package com.example.frontnodus.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.frontnodus.fragment.CalendarFragment
import com.example.frontnodus.fragment.ListFragment

class SchedulePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CalendarFragment()
            1 -> ListFragment()
            else -> CalendarFragment()
        }
    }
}
