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

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventListAdapter: EventListAdapter

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
    }

    private fun setupRecyclerView() {
        // Static all events data
        val allEvents = listOf(
            Event(
                id = 1,
                title = "Inspección de cimentación Bloque A",
                location = "Realizado por: Las Terrazas",
                date = "2025-10-25",
                time = "09:00",
                day = "25",
                monthYear = "Oct",
                status = "Pendiente"
            ),
            Event(
                id = 2,
                title = "Inspección de cimentación Bloque A",
                location = "Torre Residencial Sur",
                date = "2025-10-41",
                time = "09:00",
                day = "41",
                monthYear = "Oct",
                status = "Pendiente"
            ),
            Event(
                id = 3,
                title = "Inspección de cimentación Bloque A",
                location = "Realizado por: Las Flores",
                date = "2025-10-42",
                time = "09:00",
                day = "42",
                monthYear = "Oct",
                status = "Pendiente"
            ),
            Event(
                id = 4,
                title = "Inspección de cimentación Bloque A",
                location = "Realizado por: Las Flores",
                date = "2025-10-45",
                time = "09:00",
                day = "45",
                monthYear = "Oct",
                status = "Pendiente"
            )
        )

        eventListAdapter = EventListAdapter(allEvents) { event ->
            Toast.makeText(requireContext(), "Evento: ${event.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvAllEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventListAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
