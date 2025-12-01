package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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

class ReportIncidentActivity : AppCompatActivity() {

    private lateinit var btnCamera: LinearLayout
    private lateinit var btnVideo: LinearLayout
    private lateinit var btnSaveReport: Button
    private lateinit var ivBack: ImageView

    private var currentPhotoUri: Uri? = null
    private var currentVideoUri: Uri? = null

    // Permission Launchers
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var videoPermissionLauncher: ActivityResultLauncher<Array<String>>

    // Activity Result Launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var recordVideoLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_incident)

        // Initialize views
        btnCamera = findViewById(R.id.btnCamera)
        btnVideo = findViewById(R.id.btnVideo)
        btnSaveReport = findViewById(R.id.btnSaveReport)
        ivBack = findViewById(R.id.ivBack)

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

        btnSaveReport.setOnClickListener {
            Toast.makeText(this, "Reporte guardado", Toast.LENGTH_SHORT).show()
            finish()
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
    }

    private fun initializeActivityLaunchers() {
        // Take Picture Launcher
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                Toast.makeText(this, "Foto capturada exitosamente", Toast.LENGTH_SHORT).show()
            }
        }

        // Record Video Launcher
        recordVideoLauncher = registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success ->
            if (success) {
                Toast.makeText(this, "Video grabado exitosamente", Toast.LENGTH_SHORT).show()
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
                    "Necesitamos acceso a la cámara para tomar fotos de la incidencia."
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
                    "Necesitamos acceso a la cámara y micrófono para grabar videos de la incidencia."
                ) {
                    videoPermissionLauncher.launch(permissions)
                }
            }
            else -> {
                videoPermissionLauncher.launch(permissions)
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

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("INCIDENT_${timeStamp}_", ".jpg", storageDir)
    }

    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile("INCIDENT_VIDEO_${timeStamp}_", ".mp4", storageDir)
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
            .setMessage("El permiso de $permissionName es necesario para reportar la incidencia. Por favor, habilítalo en la configuración de la aplicación.")
            .setPositiveButton("Aceptar", null)
            .show()
    }
}
