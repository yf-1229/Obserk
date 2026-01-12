package com.obserk.ui

data class HomeUiState(
    val isStudying: Boolean = false,
    val studyTimeMinutes: Int = 0,
    val logs: List<StudyLog> = emptyList(),
    val startTimeMillis: Long? = null,
    val lastFinishedTimeMillis: Long? = null,
    val timeSinceLastStudy: String = "00:00:00"
)

data class StudyLog(
    val date: String,
    val durationMinutes: Int
)
