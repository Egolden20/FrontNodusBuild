package com.example.frontnodus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth")
data class AuthEntity(
    @PrimaryKey val id: Int = 0,
    val token: String? = null,
    val userName: String? = null
)
