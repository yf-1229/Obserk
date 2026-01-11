package com.obserk.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.obserk.data.AppDatabase
import com.obserk.data.StudyLogEntity
import com.obserk.data.StudyLogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudyLogRepository
    private val _isStudying = MutableStateFlow(false)
    private val _elapsedSeconds = MutableStateFlow(0L)
    
    // 撮影をトリガーするための Flow
    private val _captureTrigger = MutableSharedFlow<Unit>()
    val captureTrigger = _captureTrigger.asSharedFlow()

    private var timerJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyLogRepository(database.studyLogDao())
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _isStudying,
        _elapsedSeconds,
        repository.allLogs
    ) { isStudying, seconds, dbLogs ->
        HomeUiState(
            isStudying = isStudying,
            studyTimeMinutes = (seconds / 60).toInt(),
            logs = dbLogs.map { StudyLog(it.date, it.durationMinutes) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleStudying() {
        if (_isStudying.value) stopStopwatch() else startStopwatch()
    }

    private fun startStopwatch() {
        _isStudying.value = true
        _elapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
                
                // 1分ごとに撮影をトリガー (Step 8)
                if (_elapsedSeconds.value % 60 == 0L) {
                    _captureTrigger.emit(Unit)
                }
            }
        }
    }

    private fun stopStopwatch() {
        timerJob?.cancel()
        val durationMinutes = (_elapsedSeconds.value / 60).toInt()
        saveLog(if (durationMinutes > 0) durationMinutes else 1)
        
        _isStudying.value = false
        _elapsedSeconds.value = 0
    }

    private fun saveLog(minutes: Int) {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            repository.insert(StudyLogEntity(date = date, durationMinutes = minutes))
        }
    }
}
