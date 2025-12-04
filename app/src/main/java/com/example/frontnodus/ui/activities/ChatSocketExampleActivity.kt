package com.example.frontnodus.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.models.Message
import com.example.frontnodus.ui.adapters.MessageAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.frontnodus.BuildConfig
import com.example.frontnodus.R
import com.example.frontnodus.utils.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

class ChatSocketExampleActivity : AppCompatActivity() {
    private val TAG = "ChatSocketExample"
    private val okHttp = OkHttpClient()
    private val token = "" // TODO: get token from secure storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_socket_example)

        val chatId = intent.getStringExtra("chatId") ?: return

        // Init socket with backend base URL (use http->ws scheme)
        val backendHttp = BuildConfig.BACKEND_BASE_URL // e.g. http://192.168.18.12:3000/graphql
        val wsBase = backendHttp.replace("/graphql", "")

        SocketManager.init(wsBase, token)
        SocketManager.joinRoom(chatId)

        // UI bindings
        val rv = findViewById<RecyclerView>(R.id.rvMessages)
        rv.layoutManager = LinearLayoutManager(this)
        val meId = "me" // placeholder; ideally derive from token
        val adapter = MessageAdapter(meId)
        rv.adapter = adapter

        val btnSend = findViewById<Button>(R.id.btnSendExample)
        val input = findViewById<EditText>(R.id.inputExample)

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val tempId = "tmp-${UUID.randomUUID()}"
            // Show local message with tempId
            val localMsg = Message(id = null, chatId = chatId, from = meId, text = text, tempId = tempId, createdAt = "ahora")
            adapter.addMessage(localMsg)
            rv.scrollToPosition(adapter.itemCount - 1)
            SocketManager.sendMessage(chatId, text, tempId)
            Toast.makeText(this, "Enviado local (tempId=$tempId)", Toast.LENGTH_SHORT).show()
        }

        SocketManager.onNewMessage { msg ->
            Log.d(TAG, "new message: ${'$'}msg")
            try {
                val id = msg["id"] as? String
                val chat = msg["chatId"] as? String
                val from = msg["from"] as? String
                val text = msg["text"] as? String ?: (msg["content"] as? String ?: "")
                val createdAt = msg["createdAt"] as? String
                val tempId = msg["tempId"] as? String
                val message = Message(id = id, chatId = chat, from = from, text = text, tempId = tempId, createdAt = createdAt)
                adapter.addMessage(message)
                rv.post { rv.scrollToPosition(adapter.itemCount - 1) }
            } catch (e: Exception) {
                Log.e(TAG, "failed parse incoming message", e)
            }
        }

        SocketManager.onAck { ack ->
            Log.d(TAG, "ack: ${'$'}ack")
            try {
                val tempId = ack["tempId"] as? String
                val id = ack["id"] as? String
                if (!tempId.isNullOrEmpty() && !id.isNullOrEmpty()) {
                    adapter.updateMessageByTempId(tempId, id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "failed parse ack", e)
            }
        }

        // Fetch history via GraphQL
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = fetchMessages(chatId, 50, 0)
                Log.d(TAG, "history: ${'$'}messages")
                // update UI on main thread
                try {
                    val resp = JSONObject(messages)
                    val data = resp.optJSONObject("data")
                    val msgs = data?.optJSONObject("messages")?.optJSONArray("items")
                    if (msgs != null) {
                        for (i in 0 until msgs.length()) {
                            val m = msgs.getJSONObject(i)
                            val message = Message(
                                id = m.optString("id", null),
                                chatId = m.optString("chatId", null),
                                from = m.optString("from", null),
                                text = m.optString("text", ""),
                                createdAt = m.optString("createdAt", null)
                            )
                            runOnUiThread {
                                adapter.addMessage(message)
                            }
                        }
                        runOnUiThread { rv.scrollToPosition(adapter.itemCount - 1) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "parse history", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch messages error", e)
            }
        }
    }

    private fun fetchMessages(chatId: String, limit: Int, offset: Int): String {
        val query = """
            query Messages(${"$"}chatId: ID!, ${"$"}limit: Int, ${"$"}offset: Int) {
              messages(chatId: ${"$"}chatId, limit: ${"$"}limit, offset: ${"$"}offset) {
                items { id chatId from text attachments { url filename type } createdAt updatedAt }
                total
              }
            }
        """.trimIndent()

        val json = JSONObject()
        json.put("query", query)
        val vars = JSONObject()
        vars.put("chatId", chatId)
        vars.put("limit", limit)
        vars.put("offset", offset)
        json.put("variables", vars)

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder()
            .url(BuildConfig.BACKEND_BASE_URL)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val resp = okHttp.newCall(req).execute()
        val respJson = JSONObject(resp.body?.string() ?: "{}")
        return respJson.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketManager.leaveRoom(intent.getStringExtra("chatId") ?: "")
        SocketManager.disconnect()
    }
}
