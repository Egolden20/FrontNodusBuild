package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.example.frontnodus.data.repository.ProjectRepository
import com.example.frontnodus.data.repository.TaskRepository
import com.example.frontnodus.ui.adapters.ProjectCard
import com.example.frontnodus.domain.models.Task
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.data.network.FilePart
import org.json.JSONObject
import android.provider.OpenableColumns
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.BitmapFactory
import android.content.ActivityNotFoundException


class RegisterAdvanceActivity : AppCompatActivity() {

    private lateinit var btnCamera: LinearLayout
    private lateinit var btnVideo: LinearLayout
    private lateinit var btnUpload: LinearLayout
    private lateinit var btnSaveAdvance: Button
    private lateinit var ivBack: ImageView
    private lateinit var spProject: Spinner
    private lateinit var spTask: Spinner
    private var projectIds: List<String?> = emptyList()
    private var taskIds: List<String?> = emptyList()

    private var currentPhotoUri: Uri? = null
    private var currentVideoUri: Uri? = null
    private val attachments: MutableList<Uri> = mutableListOf()
    private lateinit var previewContainer: LinearLayout
    private lateinit var etActivities: android.widget.EditText
    private lateinit var checklistContainer: LinearLayout
    private val checklistCheckboxes = mutableListOf<android.widget.CheckBox>()

    // Permission Launchers
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var videoPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>

    // Activity Result Launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var recordVideoLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_advance)

        // Initialize views
        btnCamera = findViewById(R.id.btnCamera)
        btnVideo = findViewById(R.id.btnVideo)
        btnUpload = findViewById(R.id.btnUpload)
        btnSaveAdvance = findViewById(R.id.btnSaveAdvance)
        ivBack = findViewById(R.id.ivBack)
        spProject = findViewById(R.id.spProject)
        spTask = findViewById(R.id.spTask)
        previewContainer = findViewById(R.id.previewContainer)
        etActivities = findViewById(R.id.etActivities)
        checklistContainer = findViewById(R.id.checklistContainer)

        // repositories
        val projectRepository: ProjectRepository by inject()
        val taskRepository: TaskRepository by inject()

        // load projects and populate spinner
        lifecycleScope.launch {
            try {
                val projects = try { projectRepository.fetchAndCacheProjects() } catch (e: Exception) { emptyList<ProjectCard>() }
                val titles = mutableListOf<String>()
                val pIds = mutableListOf<String?>()
                titles.add("Seleccionar proyecto")
                pIds.add(null)
                for (p in projects) {
                    titles.add(p.title)
                    pIds.add(p.id)
                }
                val adapter = ArrayAdapter(this@RegisterAdvanceActivity, android.R.layout.simple_spinner_item, titles)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spProject.adapter = adapter
                projectIds = pIds

                // when project selected, fetch tasks for that project
                spProject.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                        val selectedProjectId = projectIds.getOrNull(position)
                        if (selectedProjectId != null) {
                            lifecycleScope.launch {
                                try {
                                    val tasks = try { taskRepository.getTasksByProject(selectedProjectId) } catch (e: Exception) { emptyList<Task>() }
                                    val taskTitles = mutableListOf<String>()
                                    val tIds = mutableListOf<String?>()
                                    taskTitles.add("Seleccionar tarea")
                                    tIds.add(null)
                                    for (t in tasks) {
                                        taskTitles.add(t.title)
                                        tIds.add(t.id)
                                    }
                                    val taskAdapter = ArrayAdapter(this@RegisterAdvanceActivity, android.R.layout.simple_spinner_item, taskTitles)
                                    taskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    spTask.adapter = taskAdapter
                                    taskIds = tIds
                                    // clear checklist UI when project changes
                                    runOnUiThread {
                                        checklistCheckboxes.clear()
                                        checklistContainer.removeAllViews()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            // clear tasks spinner
                            val taskAdapter = ArrayAdapter(this@RegisterAdvanceActivity, android.R.layout.simple_spinner_item, listOf("Seleccionar tarea"))
                            taskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spTask.adapter = taskAdapter
                            taskIds = listOf(null)
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                })

                // When a task is selected, fetch its full data (including checklist) and render checklist options
                spTask.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                        val selectedTaskId = taskIds.getOrNull(position)
                        if (!selectedTaskId.isNullOrBlank()) {
                            lifecycleScope.launch {
                                try {
                                    val taskJson = try { taskRepository.getTaskById(selectedTaskId) } catch (e: Exception) { null }
                                    runOnUiThread {
                                        checklistCheckboxes.clear()
                                        checklistContainer.removeAllViews()
                                        if (taskJson != null) {
                                            val checklist = taskJson.optJSONArray("checklist")
                                            if (checklist != null) {
                                                for (i in 0 until checklist.length()) {
                                                    val itemObj = checklist.optJSONObject(i)
                                                    val title = itemObj?.optString("title") ?: "Item ${i + 1}"
                                                    val completed = itemObj?.optBoolean("completed") ?: false
                                                    // Only show pending checklist items (not completed)
                                                    if (!completed) {
                                                        val cb = android.widget.CheckBox(this@RegisterAdvanceActivity)
                                                        cb.text = title
                                                        cb.isChecked = false
                                                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                                        cb.layoutParams = lp
                                                        checklistContainer.addView(cb)
                                                        checklistCheckboxes.add(cb)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            // clear checklist
                            checklistCheckboxes.clear()
                            checklistContainer.removeAllViews()
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                        checklistCheckboxes.clear()
                        checklistContainer.removeAllViews()
                    }
                })

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Back button
        ivBack.setOnClickListener {
            finish()
        }

        // Initialize permission launchers
        initializePermissionLaunchers()

        // Initialize activity result launchers
        initializeActivityLaunchers()

        // Set click listeners
        btnCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        btnVideo.setOnClickListener {
            checkVideoPermissionAndOpen()
        }

        btnUpload.setOnClickListener {
            checkStoragePermissionAndOpen()
        }

        // GraphQL client from Koin
        val graphQLClient: GraphQLClient by inject()

        btnSaveAdvance.setOnClickListener {
            // get selected project and task
            val projectPos = spProject.selectedItemPosition
            val taskPos = spTask.selectedItemPosition
            val projectSelectedId = projectIds.getOrNull(projectPos)
            val taskSelectedId = taskIds.getOrNull(taskPos)

            // Prepare payload
            lifecycleScope.launch {
                try {
                        if (projectSelectedId.isNullOrBlank()) {
                            Toast.makeText(this@RegisterAdvanceActivity, "Seleccione un proyecto antes de guardar", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // New flow: Register Advance creates a Daily report tied to a task.
                        // Require both project and task to create a daily advance.
                        if (taskSelectedId.isNullOrBlank()) {
                            Toast.makeText(this@RegisterAdvanceActivity, "Seleccione una tarea antes de guardar el avance diario", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Build variables for CreateDaily (including taskId)
                        val mutation = "mutation CreateDaily(${ '$' }input: CreateDailyInput!){ createDailyReport(input: ${ '$' }input){ id project { id } createdAt }}"
                        val vars = JSONObject()
                        val input = JSONObject()
                        input.put("project", projectSelectedId)
                        input.put("taskId", taskSelectedId)
                        // Take description/content from the activities EditText
                        input.put("content", etActivities.text.toString())

                    // Serialize checklist selected items into input.checklist (always include, attachments or not)
                    try {
                        val checklistArr = org.json.JSONArray()
                        for (cb in checklistCheckboxes) {
                            val obj = org.json.JSONObject()
                            obj.put("item", cb.text.toString())
                            // Do not mark as completed when user selects the checkbox here; selection only associates the item.
                            obj.put("completed", false)
                            // evidenceId should be a string; send empty string when none
                            obj.put("evidenceId", "")
                            checklistArr.put(obj)
                        }
                        if (checklistArr.length() > 0) {
                            input.put("checklist", checklistArr)
                        }
                    } catch (ignored: Exception) { }

                    // If there are attachments selected, perform multipart GraphQL upload per spec
                    if (attachments.isNotEmpty()) {
                        try {
                            val fileParts = mutableListOf<FilePart>()
                            // Prepare placeholders for attachmentsFiles (Upload entries) in the variables
                            val attachmentsFilesArray = org.json.JSONArray()
                            for (i in attachments.indices) {
                                attachmentsFilesArray.put(JSONObject.NULL)
                            }
                            input.put("attachmentsFiles", attachmentsFilesArray)
                            vars.put("input", input)

                            // build map: { "0": ["variables.input.attachmentsFiles.0"], ... }
                            val mapObj = JSONObject()
                            for (i in attachments.indices) {
                                val uri = attachments[i]
                                // get filename
                                var displayName = "file_$i"
                                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                                        displayName = cursor.getString(nameIndex)
                                    }
                                }
                                val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)

                                fileParts.add(FilePart(fieldName = i.toString(), filename = displayName, bytes = bytes, contentType = mime))
                                val arr = org.json.JSONArray()
                                arr.put("variables.input.attachmentsFiles.$i")
                                mapObj.put(i.toString(), arr)
                            }

                            val operationsObj = JSONObject()
                            operationsObj.put("query", mutation)
                            operationsObj.put("variables", vars)
                            // include operationName in the operations payload as well (must match the mutation name)
                            val opName = "CreateDaily"
                            operationsObj.put("operationName", opName)

                            val resp = graphQLClient.executeMultipart(operationsObj.toString(), mapObj.toString(), fileParts, opName)
                            handleCreateResponse(resp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@RegisterAdvanceActivity, "Error subiendo attachments: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // No attachments: simple JSON mutation
                        input.put("attachments", org.json.JSONArray())
                        vars.put("input", input)
                        val resp = graphQLClient.executeMutation(mutation, vars)
                        handleCreateResponse(resp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@RegisterAdvanceActivity, "Error al guardar avance: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initializePermissionLaunchers() {
        // Camera Permission Launcher
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

            if (cameraGranted) {
                openCamera()
            } else {
                showPermissionDeniedDialog("Cámara")
            }
        }

        // Video Permission Launcher
        videoPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraGranted && audioGranted) {
                openVideoRecorder()
            } else {
                showPermissionDeniedDialog("Cámara y Audio")
            }
        }

        // Storage Permission Launcher
        storagePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false ||
                permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if (granted) {
                openFilePicker()
            } else {
                showPermissionDeniedDialog("Almacenamiento")
            }
        }
    }

    private fun initializeActivityLaunchers() {
        // Take Picture Launcher
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                currentPhotoUri?.let {
                    attachments.add(it)
                    addPreviewForUri(it)
                }
                Toast.makeText(this, "Foto capturada exitosamente", Toast.LENGTH_SHORT).show()
            }
        }

        // Record Video Launcher
        recordVideoLauncher = registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success ->
            if (success) {
                currentVideoUri?.let {
                    attachments.add(it)
                    addPreviewForUri(it)
                }
                Toast.makeText(this, "Video grabado exitosamente", Toast.LENGTH_SHORT).show()
            }
        }

        // Pick File Launcher
        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                if (uri != null) {
                    attachments.add(uri)
                    addPreviewForUri(uri)
                    Toast.makeText(this, "Archivo seleccionado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show explanation and request permission
                showPermissionRationaleDialog(
                    "Cámara",
                    "Necesitamos acceso a la cámara para tomar fotos de evidencia."
                ) {
                    cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            }
            else -> {
                // Request permission directly
                cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }

    private fun checkVideoPermissionAndOpen() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        when {
            allGranted -> {
                openVideoRecorder()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionRationaleDialog(
                    "Cámara y Audio",
                    "Necesitamos acceso a la cámara y micrófono para grabar videos."
                ) {
                    videoPermissionLauncher.launch(permissions)
                }
            }
            else -> {
                videoPermissionLauncher.launch(permissions)
            }
        }
    }

    private fun checkStoragePermissionAndOpen() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        when {
            allGranted -> {
                openFilePicker()
            }
            else -> {
                storagePermissionLauncher.launch(permissions)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun openVideoRecorder() {
        try {
            val videoFile = createVideoFile()
            currentVideoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                videoFile
            )
            recordVideoLauncher.launch(currentVideoUri!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir la grabadora de video", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "image/*",
                "video/*",
                "application/pdf"
            ))
        }
        pickFileLauncher.launch(intent)
    }

    private fun addPreviewForUri(uri: Uri) {
        try {
            val mime = contentResolver.getType(uri) ?: ""

            // Use dp->px conversion to avoid relying on an external dimen resource
            val sizeDp = 64
            val size = (sizeDp * resources.displayMetrics.density + 0.5f).toInt()
            // Container so we can overlay a delete button
            val container = android.widget.FrameLayout(this)
            val containerLp = LinearLayout.LayoutParams(size, size)
            containerLp.setMargins(8, 8, 8, 8)
            container.layoutParams = containerLp

            val iv = ImageView(this)
            val ivLp = android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
            iv.layoutParams = ivLp
            iv.scaleType = ImageView.ScaleType.CENTER_CROP

            if (mime.startsWith("image")) {
                try {
                    val `is` = contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(`is`)
                    `is`?.close()
                    if (bmp != null) {
                        iv.setImageBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
                    } else {
                        iv.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                } catch (e: Exception) {
                    iv.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            } else if (mime.startsWith("video")) {
                iv.setImageResource(android.R.drawable.ic_media_play)
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_save)
            }

            // Delete button (small X) overlay
            val closeBtn = android.widget.ImageButton(this)
            val closeSizeDp = 20
            val closeSize = (closeSizeDp * resources.displayMetrics.density + 0.5f).toInt()
            val closeLp = android.widget.FrameLayout.LayoutParams(closeSize, closeSize)
            closeLp.gravity = android.view.Gravity.END or android.view.Gravity.TOP
            closeBtn.layoutParams = closeLp
            closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            closeBtn.setBackgroundResource(0)
            closeBtn.scaleType = ImageView.ScaleType.CENTER

            // Click to open inside app
            iv.setOnClickListener {
                try {
                    val intent = Intent(this@RegisterAdvanceActivity, AttachmentViewerActivity::class.java).apply {
                        putExtra("uri", uri.toString())
                        putExtra("mime", mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    openUriWithExternalApp(uri)
                }
            }

            // Delete handler: remove from attachments list and from UI
            closeBtn.setOnClickListener {
                try {
                    // remove first occurrence of this uri
                    val removed = attachments.remove(uri)
                    previewContainer.removeView(container)
                    if (removed) {
                        Toast.makeText(this, "Adjunto eliminado", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // assemble
            container.addView(iv)
            container.addView(closeBtn)
            previewContainer.addView(container)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openUriWithExternalApp(uri: Uri) {
        try {
            val mime = contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir con"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No hay aplicación disponible para abrir este archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile("VIDEO_${timeStamp}_", ".mp4", storageDir)
    }

    private fun showPermissionRationaleDialog(
        permissionName: String,
        message: String,
        onPositive: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage(message)
            .setPositiveButton("Aceptar") { _, _ -> onPositive() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPermissionDeniedDialog(permissionName: String) {
        AlertDialog.Builder(this)
            .setTitle("Permiso denegado")
            .setMessage("El permiso de $permissionName es necesario para esta funcionalidad. Por favor, habilítalo en la configuración de la aplicación.")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun handleCreateResponse(resp: JSONObject) {
        // Log full response for debugging
        android.util.Log.d("RegisterAdvance", "GraphQL response: ${resp.toString()}")

        // Check for GraphQL errors
        if (resp.has("errors")) {
            val errs = resp.optJSONArray("errors")
            val firstMsg = errs?.optJSONObject(0)?.optString("message") ?: "Error en servidor"
            runOnUiThread {
                // Show a dialog with full response so user can copy it for debugging
                try {
                    AlertDialog.Builder(this)
                        .setTitle("Error al crear reporte")
                        .setMessage("$firstMsg\n\nRespuesta completa:\n${resp.toString(2)}")
                        .setPositiveButton("Copiar") { _, _ ->
                            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                            val clip = android.content.ClipData.newPlainText("GraphQL response", resp.toString())
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Respuesta copiada al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cerrar", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al crear reporte: $firstMsg", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        val data = resp.optJSONObject("data")
        if (data != null) {
            // success — show toast and finish
            runOnUiThread {
                Toast.makeText(this, "Avance guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Respuesta inesperada del servidor")
                    .setMessage(resp.toString())
                    .setPositiveButton("Copiar") { _, _ ->
                        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                        val clip = android.content.ClipData.newPlainText("GraphQL response", resp.toString())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Respuesta copiada al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
        }
    }
}
