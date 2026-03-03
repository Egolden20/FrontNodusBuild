package com.example.frontnodus.utils

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import java.net.URISyntaxException
import org.json.JSONObject

object SocketManager {
    private var socket: Socket? = null
    private const val TAG = "SocketManager"
    private var currentUserId: String? = null
    private val userStatusCallbacks = mutableListOf<(String, String) -> Unit>()
    
    // Store single callbacks for message events to prevent accumulation
    private var messageNewCallback: ((Map<String, Any>) -> Unit)? = null
    private var messageAckCallback: ((Map<String, Any>) -> Unit)? = null
    private var messageListenersRegistered = false

    fun init(baseUrl: String, token: String, userId: String? = null) {
        if (socket != null) return
        currentUserId = userId
        try {
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            // Send Authorization header in handshake so server can validate JWT
            val headers = mutableMapOf<String, List<String>>()
            headers["Authorization"] = listOf("Bearer $token")
            opts.extraHeaders = headers
            // Also attach token in query string for compatibility with tunnels that may strip headers
            if (token.isNotBlank()) {
                try {
                    opts.query = "token=$token"
                } catch (e: Exception) {
                    Log.w(TAG, "failed to set opts.query", e)
                }
            }

            socket = IO.socket(baseUrl, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected: ${socket?.id()}")
                // Emit user presence as online
                emitPresence("online")
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d(TAG, "Socket disconnected: ${args?.getOrNull(0)}")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                try {
                    val first = args?.getOrNull(0)
                    Log.e(TAG, "connect_error: ${first?.toString()}")
                } catch (e: Exception) {
                    Log.e(TAG, "connect_error: unknown", e)
                }
            }

            // NOTE: do not auto-connect here. Connection is performed on demand via connect().
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid socket URL", e)
        }
    }

    fun isInitialized(): Boolean = socket != null

    fun connect() {
        try {
            if (socket != null && socket?.connected() != true) {
                socket?.connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect error", e)
        }
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun joinRoom(roomId: String) {
        socket?.emit("join", roomId)
    }

    fun leaveRoom(roomId: String) {
        socket?.emit("leave", roomId)
    }

    fun sendMessage(roomId: String, content: String, tempId: String = "") {
        try {
            val payload = JSONObject()
            payload.put("room", roomId)
            payload.put("content", content)
            payload.put("tempId", tempId)
            Log.d(TAG, "emit message:send -> payload=$payload")
            socket?.emit("message:send", payload)
        } catch (e: Exception) {
            Log.e(TAG, "emit error", e)
        }
    }

    fun onNewMessage(callback: (Map<String, Any>) -> Unit) {
        messageNewCallback = callback
        
        // Only register the socket listener once
        if (!messageListenersRegistered) {
            messageListenersRegistered = true
            Log.d(TAG, "Registering socket listeners for message:new (first time)")
            
            socket?.on("message:new") { args ->
                Log.d(TAG, "on message:new raw args=${args?.toList()}")
                try {
                    val obj = args?.getOrNull(0)
                    Log.d(TAG, "on message:new obj type=${obj?.javaClass?.name}, value=$obj")
                    
                    val msgMap = when (obj) {
                        is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            obj as Map<String, Any>
                        }
                        is org.json.JSONObject -> {
                            // Convert JSONObject to Map
                            val map = mutableMapOf<String, Any>()
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next() as String
                                map[key] = obj.get(key)
                            }
                            Log.d(TAG, "on message:new converted from JSONObject: $map")
                            map
                        }
                        else -> {
                            Log.w(TAG, "on message:new unexpected type, wrapping in map")
                            mapOf("raw" to (obj ?: ""))
                        }
                    }
                    
                    // Invoke the current callback
                    messageNewCallback?.invoke(msgMap)
                } catch (e: Exception) {
                    Log.e(TAG, "on message:new parse error", e)
                }
            }
        } else {
            Log.d(TAG, "Socket listener already registered, just updating callback")
        }
    }

    fun onAck(callback: (Map<String, Any>) -> Unit) {
        messageAckCallback = callback
        
        // Only register once - subsequent calls just update the callback
        if (!messageListenersRegistered) {
            messageListenersRegistered = true
            Log.d(TAG, "Registering socket listeners for message:ack (first time)")
            
            socket?.on("message:ack") { args ->
                Log.d(TAG, "on message:ack raw args=${args?.toList()}")
                try {
                    val obj = args?.getOrNull(0)
                    Log.d(TAG, "on message:ack obj type=${obj?.javaClass?.name}, value=$obj")
                    
                    val ackMap = when (obj) {
                        is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            obj as Map<String, Any>
                        }
                        is org.json.JSONObject -> {
                            val map = mutableMapOf<String, Any>()
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next() as String
                                map[key] = obj.get(key)
                            }
                            Log.d(TAG, "on message:ack converted from JSONObject: $map")
                            map
                        }
                        else -> {
                            Log.w(TAG, "on message:ack unexpected type")
                            mapOf("raw" to (obj ?: ""))
                        }
                    }
                    
                    messageAckCallback?.invoke(ackMap)
                } catch (e: Exception) {
                    Log.e(TAG, "on message:ack parse error", e)
                }
            }

            // Listen for server-side errors on message send
            socket?.on("message:error") { args ->
                try {
                    val first = args?.getOrNull(0)
                    Log.e(TAG, "message:error received: ${first?.toString()}")
                } catch (e: Exception) {
                    Log.e(TAG, "message:error parse failed", e)
                }
            }
        } else {
            Log.d(TAG, "Socket listener already registered, just updating ack callback")
        }
    }

    fun emitPresence(status: String) {
        try {
            if (currentUserId != null) {
                val payload = JSONObject()
                payload.put("status", status)
                Log.d(TAG, "emit user:presence -> status=$status")
                socket?.emit("user:presence", payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "emit presence error", e)
        }
    }

    fun onUserStatusChanged(callback: (userId: String, status: String) -> Unit) {
        // Clear old callbacks and socket listeners to avoid duplicates
        userStatusCallbacks.clear()
        socket?.off("user:status:changed")
        
        userStatusCallbacks.add(callback)
        socket?.on("user:status:changed") { args ->
            try {
                Log.d(TAG, "user:status:changed raw args=${args?.toList()}")
                val obj = args.getOrNull(0)
                Log.d(TAG, "user:status:changed obj type=${obj?.javaClass?.name}, value=$obj")
                
                when (obj) {
                    is Map<*, *> -> {
                        val userId = obj["userId"] as? String ?: ""
                        val status = obj["status"] as? String ?: "offline"
                        Log.d(TAG, "user status changed: userId=$userId status=$status")
                        userStatusCallbacks.forEach { it(userId, status) }
                    }
                    is org.json.JSONObject -> {
                        val userId = obj.optString("userId", "")
                        val status = obj.optString("status", "offline")
                        Log.d(TAG, "user status changed (from JSON): userId=$userId status=$status")
                        userStatusCallbacks.forEach { it(userId, status) }
                    }
                    else -> {
                        Log.w(TAG, "user:status:changed unexpected type")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "parse user:status:changed error", e)
            }
        }
    }

    fun setAway() {
        emitPresence("away")
    }

    fun setOnline() {
        emitPresence("online")
    }

    fun requestParticipantsStatus(chatId: String) {
        try {
            val payload = JSONObject()
            payload.put("chatId", chatId)
            Log.d(TAG, "emit request:participants:status -> chatId=$chatId")
            socket?.emit("request:participants:status", payload)
        } catch (e: Exception) {
            Log.e(TAG, "request participants status error", e)
        }
    }

    fun onParticipantsStatus(callback: (chatId: String, statuses: List<Pair<String, String>>) -> Unit) {
        socket?.off("participants:status")
        socket?.on("participants:status") { args ->
            try {
                Log.d(TAG, "participants:status raw args=${args?.toList()}")
                val obj = args.getOrNull(0)
                Log.d(TAG, "participants:status obj type=${obj?.javaClass?.name}, value=$obj")
                
                when (obj) {
                    is Map<*, *> -> {
                        val chatId = obj["chatId"] as? String ?: ""
                        val statusesArray = obj["statuses"] as? List<*>
                        val statuses = mutableListOf<Pair<String, String>>()
                        
                        statusesArray?.forEach { item ->
                            if (item is Map<*, *>) {
                                val userId = item["userId"] as? String ?: ""
                                val status = item["status"] as? String ?: "offline"
                                if (userId.isNotEmpty()) {
                                    statuses.add(Pair(userId, status))
                                }
                            }
                        }
                        
                        Log.d(TAG, "participants:status chatId=$chatId, ${statuses.size} participants")
                        statuses.forEach { (userId, status) ->
                            Log.d(TAG, "  participant: userId=$userId, status=$status")
                        }
                        callback(chatId, statuses)
                    }
                    is org.json.JSONObject -> {
                        val chatId = obj.optString("chatId", "")
                        val statusesArray = obj.optJSONArray("statuses")
                        val statuses = mutableListOf<Pair<String, String>>()
                        
                        if (statusesArray != null) {
                            for (i in 0 until statusesArray.length()) {
                                val item = statusesArray.getJSONObject(i)
                                val userId = item.optString("userId", "")
                                val status = item.optString("status", "offline")
                                if (userId.isNotEmpty()) {
                                    statuses.add(Pair(userId, status))
                                }
                            }
                        }
                        
                        Log.d(TAG, "participants:status (JSON) chatId=$chatId, ${statuses.size} participants")
                        statuses.forEach { (userId, status) ->
                            Log.d(TAG, "  participant: userId=$userId, status=$status")
                        }
                        callback(chatId, statuses)
                    }
                    else -> {
                        Log.w(TAG, "participants:status unexpected type")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "parse participants:status error", e)
            }
        }
    }

    fun disconnect() {
        emitPresence("offline")
        socket?.disconnect()
        socket?.off()
        socket = null
        currentUserId = null
        userStatusCallbacks.clear()
        messageNewCallback = null
        messageAckCallback = null
        messageListenersRegistered = false
    }

    fun clearMessageListeners() {
        Log.d(TAG, "Clearing message callbacks (not removing socket listeners)")
        messageNewCallback = null
        messageAckCallback = null
    }
}
