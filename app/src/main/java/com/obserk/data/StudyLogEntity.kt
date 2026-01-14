package com.obserk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val durationMinutes: Int,
    val imagePath: String? = null,
    val label: String? = null,
    val mlResult: String? = null // Step: ML 解析結果（ペンの持ち方の判定など）
)

@Entity(tableName = "study_labels")
data class StudyLabelEntity(
    @PrimaryKey
    val name: String
)
