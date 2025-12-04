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
import com.example.frontnodus.ui.adapters.FileAdapter
import com.example.frontnodus.databinding.FragmentFilesBinding
import com.example.frontnodus.domain.models.FileItem

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileAdapter: FileAdapter
    private val taskRepository: TaskRepository by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFilesFromBackend()
    }

    private fun loadFilesFromBackend() {
        val taskId = arguments?.getString("TASK_ID")
        val filesList = mutableListOf<com.example.frontnodus.domain.models.FileItem>()

        if (taskId.isNullOrBlank()) {
            fileAdapter = FileAdapter(filesList) { file ->
                Toast.makeText(requireContext(), "Descargando: ${file.fileName}", Toast.LENGTH_SHORT).show()
            }
            binding.rvFiles.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = fileAdapter
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val taskJson = taskRepository.getTaskById(taskId)
                val arr: JSONArray? = taskJson?.optJSONArray("attachments")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val url = arr.optString(i, "")
                        val fileName = url.substringAfterLast('/')
                        filesList.add(com.example.frontnodus.domain.models.FileItem(i + 1, fileName, "", ""))
                    }
                }
            } catch (e: Exception) {
                // ignore
            }

            fileAdapter = FileAdapter(filesList) { file ->
                Toast.makeText(requireContext(), "Descargando: ${file.fileName}", Toast.LENGTH_SHORT).show()
            }

            binding.rvFiles.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = fileAdapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
