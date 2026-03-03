package com.example.frontnodus.domain.models

data class FileItem(
    val id: String,
    val fileName: String,
    val title: String,
    val description: String?,
    val fileUrl: String,
    val uploadDate: String,
    val source: String // "incident" or "advance"
)
