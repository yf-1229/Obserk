package com.obserk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val durationMinutes: Int,
    val audioPath: String? = null,
    val label: String? = null // 学習内容のラベル
)

@Entity(tableName = "study_labels")
data class StudyLabelEntity(
    @PrimaryKey
    val name: String
)
