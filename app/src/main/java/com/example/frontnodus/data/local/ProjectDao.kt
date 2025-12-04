package com.example.frontnodus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    suspend fun getAll(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProjectEntity>)

    @Query("DELETE FROM projects")
    suspend fun clearAll()

    @Query("DELETE FROM projects WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)
}
