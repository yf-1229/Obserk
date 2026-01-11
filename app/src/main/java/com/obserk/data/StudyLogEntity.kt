package com.obserk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val durationMinutes: Int,
    val imagePath: String? = null // Step 8: 撮影した画像のパスを保存
)
