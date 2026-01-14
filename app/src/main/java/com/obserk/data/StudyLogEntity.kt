package com.obserk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,            // 表示用の日付文字列
    val startTime: Long,         // 開始時刻（ミリ秒）
    val endTime: Long? = null,   // 終了時刻（ミリ秒）
    val durationMinutes: Int = 0, // 学習時間（分）
    val totalElapsedMinutes: Int = 0,
    val efficiency: Float = 0f,   // 集中度・効率
    val mlResult: String = ""    // ML解析結果のテキスト
)