package com.obserk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Query("SELECT * FROM study_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<StudyLogEntity>>

    @Insert
    suspend fun insert(log: StudyLogEntity)

    @Query("DELETE FROM study_logs")
    suspend fun deleteAll()
}
