package com.example.frontnodus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.ui.adapters.ChecklistAdapter
import com.example.frontnodus.databinding.FragmentChecklistBinding
import com.example.frontnodus.domain.models.ChecklistItem

class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private lateinit var checklistAdapter: ChecklistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Static checklist data
        val checklistItems = mutableListOf(
            ChecklistItem(1, "Mantener niveles y cotas", true),
            ChecklistItem(2, "Revisar encofrado y apuntalamiento", true),
            ChecklistItem(3, "Inspección de acero de refuerzo", true),
            ChecklistItem(4, "Coordinación con proveedor de concreto", false)
        )

        checklistAdapter = ChecklistAdapter(checklistItems) { item, isChecked ->
            val status = if (isChecked) "completado" else "pendiente"
            Toast.makeText(requireContext(), "${item.text} - $status", Toast.LENGTH_SHORT).show()
        }

        binding.rvChecklist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = checklistAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
