package com.example.frontnodus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.adapter.FileAdapter
import com.example.frontnodus.databinding.FragmentFilesBinding
import com.example.frontnodus.model.FileItem

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileAdapter: FileAdapter

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
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Static files data
        val files = listOf(
            FileItem(1, "Plano_Losa_N2_Rev2.pdf", "12 MB", "26 Oct 2025"),
            FileItem(2, "Especificaciones_Concreto.pdf", "5.2 MB", "25 Oct 2025"),
            FileItem(3, "Foto_encofrado_1.jpg", "3.1 MB", "24 Oct 2025")
        )

        fileAdapter = FileAdapter(files) { file ->
            Toast.makeText(requireContext(), "Descargando: ${file.fileName}", Toast.LENGTH_SHORT).show()
        }

        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
