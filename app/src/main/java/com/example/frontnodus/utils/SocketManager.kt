package com.example.frontnodus.utils

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import java.net.URISyntaxException

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

            socket = IO.socket(baseUrl, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected: ${'$'}{socket?.id()}")
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d(TAG, "Socket disconnected: ${'$'}{args?.getOrNull(0)}")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "connect_error: ${'$'}{args?.getOrNull(0)}")
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid socket URL", e)
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
        val payload = mapOf(
            "room" to roomId,
            "content" to content,
            "tempId" to tempId
        )
        socket?.emit("message:send", payload)
    }

    fun onNewMessage(callback: (Map<String, Any>) -> Unit) {
        socket?.on("message:new") { args ->
            val obj = args.getOrNull(0)
            if (obj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                callback(obj as Map<String, Any>)
            } else {
                // Some clients deliver JSONObjects; convert if needed
                callback(mapOf("raw" to (obj ?: "")))
            }
        }
    }

    fun onAck(callback: (Map<String, Any>) -> Unit) {
        socket?.on("message:ack") { args ->
            val obj = args.getOrNull(0)
            if (obj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                callback(obj as Map<String, Any>)
            } else {
                callback(mapOf("raw" to (obj ?: "")))
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}
