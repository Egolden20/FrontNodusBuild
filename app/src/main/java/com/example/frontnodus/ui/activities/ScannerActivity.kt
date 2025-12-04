package com.example.frontnodus.ui.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Scanner - en construcción"
            textSize = 18f
            setPadding(24, 24, 24, 24)
        }
        setContentView(tv)
        title = "Scanner"
    }
}
