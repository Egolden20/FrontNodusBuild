package com.example.frontnodus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {
    @Query("SELECT token FROM auth WHERE id = 0")
    fun tokenFlow(): Flow<String?>

    @Query("SELECT userName FROM auth WHERE id = 0")
    fun userNameFlow(): Flow<String?>

    @Query("SELECT * FROM auth WHERE id = 0 LIMIT 1")
    suspend fun getAuth(): AuthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(auth: AuthEntity)

    @Query("UPDATE auth SET token = NULL WHERE id = 0")
    suspend fun clearToken()

    @Query("UPDATE auth SET userName = NULL WHERE id = 0")
    suspend fun clearUserName()
}
