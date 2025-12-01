package com.example.frontnodus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.ui.adapters.EventAdapter
import com.example.frontnodus.databinding.FragmentCalendarBinding
import com.example.frontnodus.domain.models.Event

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Static upcoming events data
        val upcomingEvents = listOf(
            Event(
                id = 1,
                title = "Inspección de calidad estructural",
                location = "Edificio Las Robles",
                date = "2025-10-25",
                time = "09:00",
                day = "2",
                monthYear = "Oct 25",
                status = "Pendiente"
            ),
            Event(
                id = 2,
                title = "Revisión Torre nivel 2",
                location = "Edificio Las Robles",
                date = "2025-10-28",
                time = "07:00",
                day = "4",
                monthYear = "Oct 28",
                status = "Pendiente"
            )
        )

        eventAdapter = EventAdapter(upcomingEvents) { event ->
            Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvUpcomingEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
