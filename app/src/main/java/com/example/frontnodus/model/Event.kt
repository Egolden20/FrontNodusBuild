package com.example.frontnodus.model

data class Event(
    val id: Int,
    val title: String,
    val location: String,
    val date: String,
    val time: String,
    val day: String,
    val monthYear: String,
    val status: String
)
