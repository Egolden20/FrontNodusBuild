package com.example.frontnodus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.ui.adapters.EventListAdapter
import com.example.frontnodus.databinding.FragmentListBinding
import com.example.frontnodus.domain.models.Event
import com.example.frontnodus.data.repository.EventRepository
import org.koin.android.ext.android.inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventListAdapter: EventListAdapter
    private val eventRepository: EventRepository by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadEvents()
    }

    private fun setupRecyclerView() {
        eventListAdapter = EventListAdapter(emptyList()) { event ->
            Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvAllEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventListAdapter
        }
    }

    private fun loadEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = eventRepository.getEventsForUser()
                Log.d("ListFrag", "loaded events count=${events.size}")

                // update adapter with fetched events
                eventListAdapter = EventListAdapter(events) { event ->
                    Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
                }
                binding.rvAllEvents.adapter = eventListAdapter
            } catch (e: Exception) {
                Log.e("ListFrag", "error loading events", e)
                Toast.makeText(requireContext(), e.message ?: "Error cargando eventos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
