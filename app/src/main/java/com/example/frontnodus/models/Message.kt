package com.example.frontnodus.models

data class Message(
    val id: String? = null,
    val chatId: String? = null,
    val from: String? = null,
    val text: String,
    val tempId: String? = null,
    val createdAt: String? = null
)
