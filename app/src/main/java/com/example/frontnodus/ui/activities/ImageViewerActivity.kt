package com.example.frontnodus.ui.activities

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.frontnodus.R
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.io.FileOutputStream

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutError: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnDownload: Button
    private lateinit var btnShare: Button
    private lateinit var tvImageTitle: TextView
    private lateinit var tvImageSubtitle: TextView
    private lateinit var toolbar: Toolbar

    private var imageUrl: String = ""
    private var imageTitle: String = ""
    private var imageDescription: String = ""
    private var fileName: String = ""

    companion object {
        private const val EXTRA_IMAGE_URL = "image_url"
        private const val EXTRA_IMAGE_TITLE = "image_title"
        private const val EXTRA_IMAGE_DESCRIPTION = "image_description"
        private const val EXTRA_FILE_NAME = "file_name"

        fun newIntent(
            context: Context,
            imageUrl: String,
            title: String,
            description: String,
            fileName: String
        ): Intent {
            return Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_IMAGE_TITLE, title)
                putExtra(EXTRA_IMAGE_DESCRIPTION, description)
                putExtra(EXTRA_FILE_NAME, fileName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Get data from intent
        imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: ""
        imageTitle = intent.getStringExtra(EXTRA_IMAGE_TITLE) ?: "Imagen"
        imageDescription = intent.getStringExtra(EXTRA_IMAGE_DESCRIPTION) ?: ""
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "image.jpg"

        initViews()
        setupToolbar()
        setupListeners()
        loadImage()
    }

    private fun initViews() {
        photoView = findViewById(R.id.photoView)
        progressBar = findViewById(R.id.progressBar)
        layoutError = findViewById(R.id.layoutError)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
        btnDownload = findViewById(R.id.btnDownload)
        btnShare = findViewById(R.id.btnShare)
        tvImageTitle = findViewById(R.id.tvImageTitle)
        tvImageSubtitle = findViewById(R.id.tvImageSubtitle)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        tvImageTitle.text = imageTitle
        tvImageSubtitle.text = if (imageDescription.isNotEmpty()) imageDescription else fileName
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        btnRetry.setOnClickListener {
            loadImage()
        }

        btnDownload.setOnClickListener {
            downloadImage()
        }

        btnShare.setOnClickListener {
            shareImage()
        }
    }

    private fun loadImage() {
        showLoading()

        Glide.with(this)
            .load(imageUrl)
            .error(android.R.drawable.ic_dialog_alert)
            .into(object : com.bumptech.glide.request.target.CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                ) {
                    photoView.setImageDrawable(resource)
                    showImage()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    showError("No se pudo cargar la imagen.\nVerifica tu conexión.")
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // No hacer nada
                }
            })
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        photoView.visibility = View.GONE
        layoutError.visibility = View.GONE
    }

    private fun showImage() {
        progressBar.visibility = View.GONE
        photoView.visibility = View.VISIBLE
        layoutError.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        photoView.visibility = View.GONE
        layoutError.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }

    private fun downloadImage() {
        try {
            val request = DownloadManager.Request(Uri.parse(imageUrl))
                .setTitle(fileName)
                .setDescription("Descargando $imageTitle")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Descarga iniciada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareImage() {
        // Primero descargamos la imagen a cache para compartirla
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    try {
                        val cachePath = File(cacheDir, "images")
                        cachePath.mkdirs()
                        val file = File(cachePath, fileName)
                        val fileOutputStream = FileOutputStream(file)
                        resource.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
                        fileOutputStream.flush()
                        fileOutputStream.close()

                        val contentUri = FileProvider.getUriForFile(
                            this@ImageViewerActivity,
                            "${packageName}.fileprovider",
                            file
                        )

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_TEXT, imageTitle)
                            type = "image/jpeg"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@ImageViewerActivity,
                            "Error al compartir: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // No hacer nada
                }
            })
    }
}
