package com.example.frontnodus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.repository.TaskRepository
import org.json.JSONArray
import com.example.frontnodus.ui.adapters.ChecklistAdapter
import com.example.frontnodus.databinding.FragmentChecklistBinding
import com.example.frontnodus.domain.models.ChecklistItem

class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private lateinit var checklistAdapter: ChecklistAdapter
    private val taskRepository: TaskRepository by inject()

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
        loadChecklistFromBackend()
    }

    private fun loadChecklistFromBackend() {
        val taskId = arguments?.getString("TASK_ID")
        val checklistItems = mutableListOf<ChecklistItem>()

        if (taskId.isNullOrBlank()) {
            // nothing to load
            checklistAdapter = ChecklistAdapter(checklistItems) { item, isChecked ->
                // optimistic update already applied to item by adapter
                // send updated checklist to backend
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val current = checklistItems.map { Pair(it.text, it.isChecked) }
                        taskRepository.updateChecklist(taskId ?: "", current)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error actualizando checklist", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            binding.rvChecklist.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = checklistAdapter
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val taskJson = taskRepository.getTaskById(taskId)
                val arr: JSONArray? = taskJson?.optJSONArray("checklist")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val it = arr.optJSONObject(i) ?: continue
                        val title = it.optString("title", "")
                        val completed = it.optBoolean("completed", false)
                        checklistItems.add(ChecklistItem(i + 1, title, completed))
                    }
                }
            } catch (e: Exception) {
                // ignore, show empty
            }

            checklistAdapter = ChecklistAdapter(checklistItems) { item, isChecked ->
                // optimistic UI already updated by adapter; persist change to backend
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val current = checklistItems.map { Pair(it.text, it.isChecked) }
                        val resp = taskRepository.updateChecklist(taskId, current)
                        val updated = resp?.optJSONArray("checklist")
                        if (updated != null) {
                            checklistItems.clear()
                            for (i in 0 until updated.length()) {
                                val it = updated.optJSONObject(i) ?: continue
                                val title = it.optString("title", "")
                                val completed = it.optBoolean("completed", false)
                                checklistItems.add(ChecklistItem(i + 1, title, completed))
                            }
                            checklistAdapter.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error actualizando checklist", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.rvChecklist.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = checklistAdapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
