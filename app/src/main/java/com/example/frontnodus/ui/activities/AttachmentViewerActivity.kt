package com.example.frontnodus.ui.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.frontnodus.R
import java.io.IOException

class AttachmentViewerActivity : AppCompatActivity() {

    private lateinit var ivAttachment: ImageView
    private lateinit var vvAttachment: VideoView
    private lateinit var btnClose: ImageButton
    private lateinit var pdfControls: android.widget.LinearLayout
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageInfo: TextView

    private var pdfRenderer: PdfRenderer? = null
    private var currentPageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attachment_viewer)

        ivAttachment = findViewById(R.id.ivAttachment)
        vvAttachment = findViewById(R.id.vvAttachment)
        btnClose = findViewById(R.id.btnClose)
        pdfControls = findViewById(R.id.pdfControls)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        tvPageInfo = findViewById(R.id.tvPageInfo)

        btnClose.setOnClickListener { finish() }

        val uriString = intent.getStringExtra("uri")
        val mime = intent.getStringExtra("mime") ?: "*/*"
        if (uriString == null) {
            finish()
            return
        }

        val uri = Uri.parse(uriString)

        when {
            mime.startsWith("image") -> showImage(uri)
            mime.startsWith("video") -> showVideo(uri)
            mime == "application/pdf" || uri.toString().endsWith(".pdf") -> showPdf(uri)
            else -> showUnsupported()
        }
    }

    private fun showImage(uri: Uri) {
        ivAttachment.visibility = android.view.View.VISIBLE
        vvAttachment.visibility = android.view.View.GONE
        pdfControls.visibility = android.view.View.GONE
        try {
            val `is` = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(`is`)
            `is`?.close()
            if (bmp != null) ivAttachment.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showVideo(uri: Uri) {
        ivAttachment.visibility = android.view.View.GONE
        vvAttachment.visibility = android.view.View.VISIBLE
        pdfControls.visibility = android.view.View.GONE
        try {
            vvAttachment.setVideoURI(uri)
            val mc = android.widget.MediaController(this)
            mc.setAnchorView(vvAttachment)
            vvAttachment.setMediaController(mc)
            vvAttachment.requestFocus()
            vvAttachment.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPdf(uri: Uri) {
        ivAttachment.visibility = android.view.View.VISIBLE
        vvAttachment.visibility = android.view.View.GONE
        pdfControls.visibility = android.view.View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    pdfRenderer = PdfRenderer(pfd)
                    currentPageIndex = 0
                    renderPdfPage(currentPageIndex)
                    updatePdfControls()

                    btnPrevPage.setOnClickListener {
                        if (currentPageIndex > 0) {
                            currentPageIndex--
                            renderPdfPage(currentPageIndex)
                            updatePdfControls()
                        }
                    }

                    btnNextPage.setOnClickListener {
                        pdfRenderer?.let {
                            if (currentPageIndex + 1 < it.pageCount) {
                                currentPageIndex++
                                renderPdfPage(currentPageIndex)
                                updatePdfControls()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            // Older devices: not supported, show message
            showUnsupported()
        }
    }

    private fun updatePdfControls() {
        val total = pdfRenderer?.pageCount ?: 0
        tvPageInfo.text = "Página ${currentPageIndex + 1} / $total"
        btnPrevPage.isEnabled = currentPageIndex > 0
        btnNextPage.isEnabled = pdfRenderer != null && currentPageIndex + 1 < (pdfRenderer?.pageCount ?: 0)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun renderPdfPage(index: Int) {
        pdfRenderer?.let { renderer ->
            if (index < 0 || index >= renderer.pageCount) return
            val page = renderer.openPage(index)
            val width = resources.displayMetrics.densityDpi / 72 * page.width
            val height = resources.displayMetrics.densityDpi / 72 * page.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            ivAttachment.setImageBitmap(bitmap)
            page.close()
        }
    }

    private fun showUnsupported() {
        ivAttachment.visibility = android.view.View.VISIBLE
        vvAttachment.visibility = android.view.View.GONE
        pdfControls.visibility = android.view.View.GONE
        ivAttachment.setImageResource(android.R.drawable.ic_dialog_alert)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
