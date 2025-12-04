package com.example.frontnodus.domain.models

data class Comment(
    val id: String,
    val userName: String,
    val commentText: String,
    val timeAgo: String
)
