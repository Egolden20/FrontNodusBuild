package com.example.frontnodus.data.storage

import android.content.Context
import com.example.frontnodus.data.local.AppDatabase
import com.example.frontnodus.data.local.AuthEntity
import kotlinx.coroutines.flow.Flow

class TokenStore(private val context: Context) {
    private val db by lazy { AppDatabase.getInstance(context) }
    private val dao by lazy { db.authDao() }

    suspend fun saveToken(token: String) {
        val existing = dao.getAuth()
        val userName = existing?.userName
        dao.insert(AuthEntity(id = 0, token = token, userName = userName))
    }

    suspend fun clearToken() {
        dao.clearToken()
    }

    suspend fun saveUserName(name: String) {
        val existing = dao.getAuth()
        val token = existing?.token
        dao.insert(AuthEntity(id = 0, token = token, userName = name))
    }

    suspend fun clearUserName() {
        dao.clearUserName()
    }

    val tokenFlow: Flow<String?> = dao.tokenFlow()

    val userNameFlow: Flow<String?> = dao.userNameFlow()

    // convenience method if direct read is needed
    suspend fun getToken(): String? = dao.getAuth()?.token
}
