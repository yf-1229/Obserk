package com.obserk.ui

data class HomeUiState(
    val isStudying: Boolean = false,
    val studyTimeMinutes: Int = 0,
    val logs: List<StudyLog> = emptyList()
)

data class StudyLog(
    val date: String,
    val durationMinutes: Int
)
