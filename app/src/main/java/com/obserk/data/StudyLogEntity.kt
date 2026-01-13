package com.obserk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val durationMinutes: Int,
    val imagePath: String? = null, // 音声のパスを削除し、写真のパスに変更
    val label: String? = null
)

@Entity(tableName = "study_labels")
data class StudyLabelEntity(
    @PrimaryKey
    val name: String
)
