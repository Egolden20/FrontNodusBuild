package com.example.frontnodus.domain.models

data class Task(
    val id: Int,
    val title: String,
    val subtitle: String,
    val date: String,
    val status: String,
    val actionButton: String
)
