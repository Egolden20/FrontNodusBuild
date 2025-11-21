package com.example.frontnodus.model

data class ChecklistItem(
    val id: Int,
    val text: String,
    var isChecked: Boolean = false
)
