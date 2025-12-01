package com.example.frontnodus.domain.models

data class ChecklistItem(
    val id: Int,
    val text: String,
    var isChecked: Boolean = false
)
