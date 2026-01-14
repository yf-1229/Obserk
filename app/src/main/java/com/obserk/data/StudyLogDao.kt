package com.obserk.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)suspend fun insertLog(log: StudyLogEntity)

    @Update
    suspend fun update(log: StudyLogEntity)

    @Query("SELECT * FROM study_logs ORDER BY startTime DESC")
    fun getAllLogs(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE id = :id")
    suspend fun getLogById(id: Int): StudyLogEntity?

    @Query("DELETE FROM study_logs")
    suspend fun deleteAll()
}