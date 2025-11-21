package com.example.frontnodus.model

data class Comment(
    val id: Int,
    val userName: String,
    val commentText: String,
    val timeAgo: String,
    val status: String
)
