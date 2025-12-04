package com.example.frontnodus.ui.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.R

class ChatActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        tvTitle = findViewById(R.id.tvChatTitle)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        val userName = intent.getStringExtra("userName") ?: "Contacto"
        tvTitle.text = userName

        rvMessages.layoutManager = LinearLayoutManager(this)
        // For now, no messages adapter; this is a UI skeleton.

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener
            // Placeholder: simply show a toast and clear input
            Toast.makeText(this, "Enviar: $text", Toast.LENGTH_SHORT).show()
            etMessage.setText("")
        }
    }
}
