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
import com.example.frontnodus.data.network.GraphQLClient
import org.koin.android.ext.android.inject
import org.json.JSONObject
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"

    private lateinit var btnBack: ImageButton
    private lateinit var tvHeaderAvatar: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        btnBack = findViewById(R.id.btnBack)
        tvHeaderAvatar = findViewById(R.id.tvHeaderAvatar)
        tvTitle = findViewById(R.id.tvChatTitle)
        tvStatus = findViewById(R.id.tvChatStatus)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        // Back button functionality
        btnBack.setOnClickListener { finish() }

        // Get chatId from intent (from ChatListActivity)
        val chatId = intent.getStringExtra("chatId")
        val chatTitle = intent.getStringExtra("chatTitle") ?: "Chat del Proyecto"
        
        Log.d(TAG, "ChatActivity opened with chatId=$chatId, chatTitle=$chatTitle")
        
        tvTitle.text = chatTitle
        tvStatus.text = "En línea" // Will be updated with real presence later
        
        // Set avatar initial
        tvHeaderAvatar.text = chatTitle.firstOrNull()?.uppercase() ?: "C"

        if (chatId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Chat ID no disponible", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true  // Start from bottom
        }
        
        val tokenStore: TokenStore by inject()
        val backendHttp = BuildConfig.BACKEND_BASE_URL
        val wsBase = backendHttp.replace("/graphql", "")

        // Fetch token and setup everything
        lifecycleScope.launch {
            try {
                val savedToken = tokenStore.getToken() ?: ""
                Log.d(TAG, "fetched token length=${savedToken.length}")
                val userId = extractUserIdFromToken(savedToken)
                Log.d(TAG, "userId extracted: $userId")
                val meId = userId ?: "me"
                Log.d(TAG, "meId assigned: $meId")
                
                // Create adapter with correct userId
                val adapter = MessageAdapter(meId)
                rvMessages.adapter = adapter
                
                // Auto-scroll when keyboard appears
                adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        super.onItemRangeInserted(positionStart, itemCount)
                        rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
                    }
                })
        
                // CRITICAL: Clear any previous listeners before registering new ones
                SocketManager.clearMessageListeners()
                Log.d(TAG, "Cleared previous message listeners before registering new ones")
        
                // Register message listeners FIRST before any socket operations
                SocketManager.onNewMessage { msg ->
                    Log.d(TAG, "onNewMessage raw=$msg")
                    Log.d(TAG, "onNewMessage keys=${msg.keys}")
                    val id = msg["id"] as? String
                    val chat = msg["chatId"] as? String
                    val from = msg["from"] as? String
                    val text = msg["text"] as? String ?: (msg["content"] as? String ?: "")
                    val createdAt = msg["createdAt"] as? String
                    val tempId = msg["tempId"] as? String
                    Log.d(TAG, "onNewMessage parsed: id=$id, chatId=$chat, from=$from, text=$text, tempId=$tempId")
                    Log.d(TAG, "onNewMessage FILTER CHECK: received chatId=$chat, current chatId=$chatId, match=${chat == chatId}")
                    
                    // CRITICAL: Only process messages for THIS chat
                    if (chat != chatId) {
                        Log.w(TAG, "onNewMessage REJECTING message from different chat: received=$chat, current=$chatId")
                        return@onNewMessage
                    }
                    
                    Log.d(TAG, "onNewMessage ACCEPTED message for this chat")
                    
                    runOnUiThread {
                        // If this message has a tempId, it might already exist as a temporary message
                        // In that case, just update it instead of adding a duplicate
                        if (!tempId.isNullOrEmpty() && !id.isNullOrEmpty()) {
                            Log.d(TAG, "onNewMessage has tempId, attempting to update existing message")
                            adapter.updateMessageByTempId(tempId, id)
                        } else {
                            // No tempId means this is a new message from another user or from history
                            val message = Message(id = id, chatId = chat, from = from, text = text, tempId = tempId, createdAt = createdAt)
                            Log.d(TAG, "onNewMessage adding new message to adapter")
                            adapter.addMessage(message)
                            Log.d(TAG, "onNewMessage message added, adapter count=${adapter.itemCount}")
                        }
                    }
                }

                SocketManager.onAck { ack ->
                    Log.d(TAG, "onAck raw=$ack")
                    val tempId = ack["tempId"] as? String
                    val id = ack["messageId"] as? String ?: ack["id"] as? String
                    if (!tempId.isNullOrEmpty() && !id.isNullOrEmpty()) {
                        runOnUiThread {
                            adapter.updateMessageByTempId(tempId, id)
                        }
                    }
                }

                // Socket is already initialized globally in App.kt
                // Just ensure we're connected before joining the room
                if (!SocketManager.isConnected()) {
                    SocketManager.connect()
                    kotlinx.coroutines.delay(500) // Wait for connection to establish
                }
                SocketManager.joinRoom(chatId)
                Log.d(TAG, "joined room $chatId")
                
                // Load existing messages from database
                loadMessages(chatId, savedToken, adapter)
                
                // Setup send button
                btnSend.setOnClickListener {
                    val text = etMessage.text.toString().trim()
                    if (text.isBlank()) return@setOnClickListener
                    
                    etMessage.setText("")  // Clear immediately for better UX
                    
                    val tempId = "tmp-${UUID.randomUUID()}"
                    val localMsg = Message(
                        id = null, 
                        chatId = chatId, 
                        from = meId, 
                        text = text, 
                        tempId = tempId, 
                        createdAt = System.currentTimeMillis().toString()
                    )
                    adapter.addMessage(localMsg)
                    Log.d(TAG, "sending message local tempId=$tempId text=$text to room=$chatId")
                    
                    // Ensure connection before sending
                    lifecycleScope.launch {
                        try {
                            if (!SocketManager.isConnected()) {
                                SocketManager.connect()
                                kotlinx.coroutines.delay(500) // Wait for connection
                                SocketManager.joinRoom(chatId)
                            }
                            SocketManager.sendMessage(chatId, text, tempId)
                        } catch (e: Exception) {
                            Log.e(TAG, "failed send message via socket", e)
                            runOnUiThread {
                                Toast.makeText(this@ChatActivity, "Error enviando mensaje", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "failed setup chat", e)
            }
        }
    }

    private suspend fun loadMessages(chatId: String, token: String, adapter: MessageAdapter) {
        try {
            Log.d(TAG, "Loading messages for chat $chatId")
            val client = GraphQLClient(BuildConfig.BACKEND_BASE_URL) { token }
            
            val query = """
                query GetMessages(${"$"}chatId: ID!) {
                    messages(chatId: ${"$"}chatId) {
                        items {
                            id
                            chatId
                            from
                            text
                            createdAt
                        }
                    }
                }
            """.trimIndent()
            
            val variables = JSONObject().apply {
                put("chatId", chatId)
            }
            
            Log.d(TAG, "Loading messages for chatId=$chatId with variables=$variables")
            val response = client.executeMutation(query, variables)
            Log.d(TAG, "Messages response: $response")
            
            if (response.has("data")) {
                val data = response.getJSONObject("data")
                if (data.has("messages")) {
                    val messagesObj = data.getJSONObject("messages")
                    if (messagesObj.has("items")) {
                        val messagesArray = messagesObj.getJSONArray("items")
                        Log.d(TAG, "Found ${messagesArray.length()} messages in DB")
                        
                        runOnUiThread {
                            for (i in 0 until messagesArray.length()) {
                                val msgObj = messagesArray.getJSONObject(i)
                                val message = Message(
                                    id = msgObj.optString("id"),
                                    chatId = msgObj.optString("chatId"),
                                    from = msgObj.optString("from"),
                                    text = msgObj.optString("text"),
                                    createdAt = msgObj.optString("createdAt")
                                )
                                adapter.addMessage(message)
                            }
                            Log.d(TAG, "Loaded ${messagesArray.length()} messages to adapter")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading messages", e)
        }
    }

    private fun extractUserIdFromCurrentToken(): String? {
        return try {
            val tokenStore: TokenStore by inject()
            val token = runBlocking { tokenStore.getToken() } ?: return null
            extractUserIdFromToken(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract userId", e)
            null
        }
    }

    private fun extractUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                Log.d(TAG, "Token payload decoded: $payload")
                val json = JSONObject(payload)
                val userId = json.optString("sub").takeIf { it.isNotEmpty() }
                    ?: json.optString("id").takeIf { it.isNotEmpty() }
                    ?: json.optString("userId").takeIf { it.isNotEmpty() }
                Log.d(TAG, "Extracted userId from token: $userId")
                userId
            } else {
                Log.e(TAG, "Token has invalid format (less than 2 parts)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract userId from token", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val chatId = intent.getStringExtra("chatId")
        Log.d(TAG, "onDestroy: leaving room $chatId")
        if (!chatId.isNullOrEmpty()) {
            SocketManager.leaveRoom(chatId)
        }
        // Clear message listeners when leaving to prevent receiving messages for this chat
        SocketManager.clearMessageListeners()
    }
}

// Helper for blocking calls
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}
