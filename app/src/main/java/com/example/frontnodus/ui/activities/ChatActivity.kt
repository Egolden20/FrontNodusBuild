package com.example.frontnodus.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.frontnodus.BuildConfig
import com.example.frontnodus.R
import com.example.frontnodus.models.Message
import com.example.frontnodus.ui.adapters.MessageAdapter
import com.example.frontnodus.utils.SocketManager
import com.example.frontnodus.data.storage.TokenStore
import org.koin.android.ext.android.inject
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"

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
        val meId = "me" // TODO: derive from token
        val adapter = MessageAdapter(meId)
        rvMessages.adapter = adapter

        // Fetch token AFTER we fetch the token from TokenStore (do not init socket yet)
        val backendHttp = BuildConfig.BACKEND_BASE_URL
        val wsBase = backendHttp.replace("/graphql", "")
        val tokenStore: TokenStore by inject()
        // Determine room id: prefer explicit chatId, fallback to userId
        val chatId = intent.getStringExtra("chatId") ?: intent.getStringExtra("userId")

        // We'll fetch the token and keep it, but only init/connect the socket when the user actually sends a message.
        var savedToken: String = ""
        lifecycleScope.launch {
            try {
                savedToken = tokenStore.getToken() ?: ""
                Log.d(TAG, "fetched token length=${savedToken.length}")
            } catch (e: Exception) {
                Log.e(TAG, "failed fetch token", e)
            }
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener
            val tempId = "tmp-${UUID.randomUUID()}"
            val localMsg = Message(id = null, chatId = chatId, from = meId, text = text, tempId = tempId, createdAt = "now")
            adapter.addMessage(localMsg)
            rvMessages.scrollToPosition(adapter.itemCount - 1)
            Log.d(TAG, "sending message local tempId=$tempId text=$text to room=$chatId")
            // Ensure socket initialized and connected only when user sends a message
            lifecycleScope.launch {
                try {
                    if (!SocketManager.isInitialized()) {
                        SocketManager.init(wsBase, savedToken)
                    }
                    if (!SocketManager.isConnected()) {
                        SocketManager.connect()
                    }
                    if (!chatId.isNullOrEmpty()) {
                        SocketManager.joinRoom(chatId)
                        Log.d(TAG, "joined room $chatId")
                    }
                    SocketManager.sendMessage(chatId ?: "", text, tempId)
                } catch (e: Exception) {
                    Log.e(TAG, "failed send message via socket", e)
                }
            }
            etMessage.setText("")
        }

        SocketManager.onNewMessage { msg ->
            Log.d(TAG, "onNewMessage raw=$msg")
            val id = msg["id"] as? String
            val chat = msg["chatId"] as? String
            val from = msg["from"] as? String
            val text = msg["text"] as? String ?: (msg["content"] as? String ?: "")
            val createdAt = msg["createdAt"] as? String
            val tempId = msg["tempId"] as? String
            val message = Message(id = id, chatId = chat, from = from, text = text, tempId = tempId, createdAt = createdAt)
            runOnUiThread {
                adapter.addMessage(message)
                rvMessages.scrollToPosition(adapter.itemCount - 1)
            }
        }

        SocketManager.onAck { ack ->
            Log.d(TAG, "onAck raw=$ack")
            val tempId = ack["tempId"] as? String
            val id = ack["messageId"] as? String ?: ack["id"] as? String
            if (!tempId.isNullOrEmpty() && !id.isNullOrEmpty()) {
                adapter.updateMessageByTempId(tempId, id)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val chatId = intent.getStringExtra("chatId") ?: intent.getStringExtra("userId")
        if (!chatId.isNullOrEmpty()) SocketManager.leaveRoom(chatId)
        SocketManager.disconnect()
    }
}
