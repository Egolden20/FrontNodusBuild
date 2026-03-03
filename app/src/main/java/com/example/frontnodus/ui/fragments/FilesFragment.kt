package com.example.frontnodus.ui.fragments

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.example.frontnodus.data.network.GraphQLClient
import org.json.JSONArray
import org.json.JSONObject
import com.example.frontnodus.ui.adapters.TaskFileAdapter
import com.example.frontnodus.databinding.FragmentFilesBinding
import com.example.frontnodus.domain.models.FileItem
import com.example.frontnodus.ui.activities.AttachmentViewerActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileAdapter: TaskFileAdapter
    private val taskRepository: TaskRepository by inject()
    private val graphQLClient: GraphQLClient by inject()

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
        loadFilesFromBackend()
    }

    private fun setupRecyclerView() {
        fileAdapter = TaskFileAdapter(
            onFileClick = { file -> openFilePreview(file) },
            onDownloadClick = { file -> downloadFile(file) }
        )
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileAdapter
        }
    }

    private fun loadFilesFromBackend() {
        val taskId = arguments?.getString("TASK_ID")
        
        if (taskId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "ID de tarea no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val filesList = mutableListOf<FileItem>()
                
                // 1. Cargar incidencias de la tarea
                val incidentsQuery = """
                    query GetIncidentsByTask(${'$'}taskId: ID!) {
                        incidentsByTask(taskId: ${'$'}taskId) {
                            id
                            title
                            description
                            evidence
                            createdAt
                        }
                    }
                """.trimIndent()
                
                val incidentsVars = JSONObject().apply {
                    put("taskId", taskId)
                }
                
                val incidentsResponse = graphQLClient.executeMutation(incidentsQuery, incidentsVars)
                
                Log.d("FilesFragment", "Incidents Response: $incidentsResponse")
                
                if (incidentsResponse.has("data") && !incidentsResponse.isNull("data")) {
                    val data = incidentsResponse.getJSONObject("data")
                    if (data.has("incidentsByTask")) {
                        val incidents = data.getJSONArray("incidentsByTask")
                        for (i in 0 until incidents.length()) {
                            val incident = incidents.getJSONObject(i)
                            val evidenceArray = incident.optJSONArray("evidence")
                            if (evidenceArray != null) {
                                for (j in 0 until evidenceArray.length()) {
                                    val evidenceUrl = evidenceArray.optString(j)
                                    // Construir URL completa si es relativa
                                    val fullUrl = if (evidenceUrl.startsWith("http")) {
                                        evidenceUrl
                                    } else {
                                        // Usar la URL base del backend sin el /graphql
                                        val baseUrl = com.example.frontnodus.BuildConfig.BACKEND_BASE_URL.replace("/graphql", "")
                                        "$baseUrl$evidenceUrl"
                                    }
                                    filesList.add(FileItem(
                                        id = incident.getString("id") + "_$j",
                                        fileName = evidenceUrl.substringAfterLast('/'),
                                        title = incident.getString("title"),
                                        description = incident.optString("description", ""),
                                        fileUrl = fullUrl,
                                        uploadDate = formatDate(incident.getString("createdAt")),
                                        source = "Incidencia"
                                    ))
                                }
                            }
                        }
                    }
                }
                
                // 2. Cargar avances (reportes) de la tarea
                val reportsQuery = """
                    query GetReportsByTask(${'$'}taskId: ID!) {
                        reportsByTask(taskId: ${'$'}taskId) {
                            id
                            content
                            attachments
                            createdAt
                        }
                    }
                """.trimIndent()
                
                val reportsVars = JSONObject().apply {
                    put("taskId", taskId)
                }
                
                val reportsResponse = graphQLClient.executeMutation(reportsQuery, reportsVars)
                
                Log.d("FilesFragment", "Reports Response: $reportsResponse")
                
                if (reportsResponse.has("data") && !reportsResponse.isNull("data")) {
                    val data = reportsResponse.getJSONObject("data")
                    if (data.has("reportsByTask")) {
                        val reports = data.getJSONArray("reportsByTask")
                        for (i in 0 until reports.length()) {
                            val report = reports.getJSONObject(i)
                            val attachmentsArray = report.optJSONArray("attachments")
                            if (attachmentsArray != null) {
                                for (j in 0 until attachmentsArray.length()) {
                                    val attachmentUrl = attachmentsArray.optString(j)
                                    // Construir URL completa si es relativa
                                    val fullUrl = if (attachmentUrl.startsWith("http")) {
                                        attachmentUrl
                                    } else {
                                        // Usar la URL base del backend sin el /graphql
                                        val baseUrl = com.example.frontnodus.BuildConfig.BACKEND_BASE_URL.replace("/graphql", "")
                                        "$baseUrl$attachmentUrl"
                                    }
                                    filesList.add(FileItem(
                                        id = report.getString("id") + "_$j",
                                        fileName = attachmentUrl.substringAfterLast('/'),
                                        title = "Avance diario",
                                        description = report.optString("content", ""),
                                        fileUrl = fullUrl,
                                        uploadDate = formatDate(report.getString("createdAt")),
                                        source = "Avance"
                                    ))
                                }
                            }
                        }
                    }
                }
                
                // Actualizar adapter con todos los archivos
                Log.d("FilesFragment", "Total files loaded: ${filesList.size}")
                fileAdapter.submitList(filesList)
                
                if (filesList.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay archivos disponibles", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e("FilesFragment", "Error loading files", e)
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al cargar archivos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openFilePreview(file: FileItem) {
        try {
            // Verificar si es una imagen por la extensión
            val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
            val fileExtension = file.fileName.substringAfterLast('.', "").lowercase()
            
            if (fileExtension in imageExtensions) {
                // Abrir visor de imágenes
                val intent = com.example.frontnodus.ui.activities.ImageViewerActivity.newIntent(
                    requireContext(),
                    file.fileUrl,
                    file.title,
                    file.description ?: "",
                    file.fileName
                )
                startActivity(intent)
            } else {
                // Para otros tipos de archivos, mostrar mensaje
                Toast.makeText(requireContext(), 
                    "Tipo de archivo no soportado para vista previa.\nUsa el botón 'Descargar' para obtener el archivo", 
                    Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al abrir archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadFile(file: FileItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Descargar archivo usando DownloadManager de Android
                val request = android.app.DownloadManager.Request(Uri.parse(file.fileUrl))
                    .setTitle(file.fileName)
                    .setDescription("Descargando ${file.title}")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                        file.fileName
                    )
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val downloadManager = requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                downloadManager.enqueue(request)

                Toast.makeText(requireContext(), "Descargando ${file.fileName}...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".mp4", true) -> "video/mp4"
            else -> "*/*"
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
