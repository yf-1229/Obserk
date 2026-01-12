package com.obserk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Query("SELECT * FROM study_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<StudyLogEntity>>

    @Insert
    suspend fun insert(log: StudyLogEntity): Long

    @Update
    suspend fun update(log: StudyLogEntity)

    @Query("DELETE FROM study_logs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM study_labels")
    fun getAllLabels(): Flow<List<StudyLabelEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLabel(label: StudyLabelEntity)

    @Query("DELETE FROM study_labels WHERE name = :name")
    suspend fun deleteLabel(name: String)
}
