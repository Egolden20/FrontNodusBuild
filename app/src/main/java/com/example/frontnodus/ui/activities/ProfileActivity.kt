package com.example.frontnodus.ui.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Perfil - en construcción"
            textSize = 18f
            setPadding(24, 24, 24, 24)
        }
        setContentView(tv)
        title = "Perfil"
    }
}
