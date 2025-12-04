package com.example.frontnodus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY date ASC")
    suspend fun getByProject(projectId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE projectId = :projectId AND id NOT IN (:ids)")
    suspend fun deleteNotInForProject(projectId: String, ids: List<String>)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun clearProject(projectId: String)
}
