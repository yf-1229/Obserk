package com.obserk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,            // 表示用の日付文字列 (yyyy/MM/dd)
    val startTime: Long,         // 開始時刻（ミリ秒）
    val endTime: Long? = null,   // 終了時刻（ミリ秒）
    val durationMinutes: Int = 0, // 有効な学習時間（分）
    val totalElapsedMinutes: Int = 0, // 合計経過時間（分）
    val efficiency: Float = 0f    // 集中度・効率 (durationMinutes / totalElapsedMinutes * 100)
)
