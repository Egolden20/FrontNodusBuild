package com.example.frontnodus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.ui.adapters.EventAdapter
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.repository.EventRepository
import androidx.lifecycle.lifecycleScope
import android.util.Log
import kotlinx.coroutines.launch
import com.example.frontnodus.databinding.FragmentCalendarBinding
import com.example.frontnodus.domain.models.Event

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private val eventRepository: EventRepository by inject()

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
        loadEvents()
    }

    private fun setupRecyclerView() {
        // placeholder adapter until data arrives
        eventAdapter = EventAdapter(emptyList()) { event ->
            Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvUpcomingEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun loadEvents() {
        val projectId = arguments?.getString("PROJECT_ID")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = if (projectId.isNullOrBlank()) {
                    // fetch all events for current user (aggregated across projects)
                    eventRepository.getEventsForUser()
                } else {
                    eventRepository.getEventsByProject(projectId)
                }

                Log.d("CalendarFrag", "fetched events count=${events.size}")
                for (e in events) {
                    Log.d("CalendarFrag", "event id=${e.id} title=${e.title} status=${e.status} project=${e.location}")
                }

                // show only pending events in the calendar list
                val pendingEvents = events.filter { it.status.equals("pendiente", true) }
                Log.d("CalendarFrag", "pending events count=${pendingEvents.size}")

                if (pendingEvents.isNotEmpty()) {
                    eventAdapter = EventAdapter(pendingEvents) { event ->
                        Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
                    }
                    binding.rvUpcomingEvents.adapter = eventAdapter
                    binding.rvUpcomingEvents.visibility = View.VISIBLE
                    binding.rvUpcomingEvents.post {
                        Log.d("CalendarFrag", "rv height=${binding.rvUpcomingEvents.height} itemCount=${binding.rvUpcomingEvents.adapter?.itemCount}")
                    }
                } else {
                    // no pending events — show empty adapter and inform the user
                    eventAdapter = EventAdapter(emptyList()) { event ->
                        Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
                    }
                    binding.rvUpcomingEvents.adapter = eventAdapter
                    binding.rvUpcomingEvents.visibility = View.VISIBLE
                    binding.rvUpcomingEvents.post {
                        Log.d("CalendarFrag", "rv height=${binding.rvUpcomingEvents.height} itemCount=${binding.rvUpcomingEvents.adapter?.itemCount}")
                    }
                    Log.d("CalendarFrag", "No hay eventos pendientes to display")
                    Toast.makeText(requireContext(), "No hay eventos pendientes", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CalendarFrag", "error loading events", e)
                Toast.makeText(requireContext(), e.message ?: "Error cargando eventos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
