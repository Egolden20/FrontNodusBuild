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

    fun init(baseUrl: String, token: String) {
        if (socket != null) return
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
        socket?.on("message:new") { args ->
            Log.d(TAG, "on message:new raw args=${args?.toList()}")
            val obj = args.getOrNull(0)
            if (obj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                callback(obj as Map<String, Any>)
            } else {
                callback(mapOf("raw" to (obj ?: "")))
            }
        }
    }

    fun onAck(callback: (Map<String, Any>) -> Unit) {
        socket?.on("message:ack") { args ->
            Log.d(TAG, "on message:ack raw args=${args?.toList()}")
            val obj = args.getOrNull(0)
            if (obj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                callback(obj as Map<String, Any>)
            } else {
                callback(mapOf("raw" to (obj ?: "")))
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
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}
