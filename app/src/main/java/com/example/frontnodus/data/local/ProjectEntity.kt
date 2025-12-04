package com.example.frontnodus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val status: String?,
    val startDate: String?,
    val endDate: String?,
    val ownerName: String?,
    val role: String?
)
