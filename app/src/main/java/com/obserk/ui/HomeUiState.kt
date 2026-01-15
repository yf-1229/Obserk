package com.obserk.ui

data class HomeUiState(
    val isStudying: Boolean = false,
    val studyTimeMinutes: Int = 0,
    val logs: List<StudyLog> = emptyList(),
    val startTimeMillis: Long? = null,
    val lastFinishedTimeMillis: Long? = null,
    val labels: List<String> = emptyList(),
    val showCompletionDialog: Boolean = false,
    val editingLog: StudyLog? = null,
    val latestMlResult: String? = null
)

data class StudyLog(
    val id: Int = 0,
    val date: String,
    val durationMinutes: Int,
    val totalElapsedMinutes: Int,
    val efficiency: Float,
    val label: String? = null,
    val mlResult: String? = null
)

