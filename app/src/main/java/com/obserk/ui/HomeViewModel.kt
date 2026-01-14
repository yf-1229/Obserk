package com.obserk.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.obserk.data.AppDatabase
import com.obserk.data.StudyLogEntity
import com.obserk.data.StudyLogRepository
import com.obserk.service.StudyForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudyLogRepository
    private val _isStudying = MutableStateFlow(false)
    private val _startTimeMillis = MutableStateFlow<Long?>(null)
    private val _lastFinishedTimeMillis = MutableStateFlow<Long?>(null)
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())

    private val _showCompletionDialog = MutableStateFlow(false)
    private val _editingLog = MutableStateFlow<StudyLog?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyLogRepository(database.studyLogDao())

        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _isStudying,              // 0
        _startTimeMillis,         // 1
        _lastFinishedTimeMillis,  // 2
        _currentTimeMillis,       // 3
        repository.allLogs,       // 4
        _showCompletionDialog,    // 5
        _editingLog               // 6
    ) { params ->
        val isStudying = params[0] as Boolean
        val startTime = params[1] as Long?
        val lastFinished = params[2] as Long?
        val currentTime = params[3] as Long
        val dbLogs = params[4] as List<StudyLogEntity>
        val showCompletion = params[5] as Boolean
        val editingLog = params[6] as StudyLog?

        val timeSinceLast = if (!isStudying && lastFinished != null) {
            formatDuration(currentTime - lastFinished)
        } else {
            "00:00:00"
        }

        HomeUiState(
            isStudying = isStudying,
            startTimeMillis = startTime,
            lastFinishedTimeMillis = lastFinished,
            timeSinceLastStudy = timeSinceLast,
            logs = dbLogs.map { StudyLog(it.id, it.date, it.durationMinutes, it.totalElapsedMinutes, it.efficiency) },
            showCompletionDialog = showCompletion,
            editingLog = editingLog
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
        _startTimeMillis.value = System.currentTimeMillis()
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, StudyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopStopwatch() {
        val now = System.currentTimeMillis()
        val durationMillis = _startTimeMillis.value?.let { now - it } ?: 0L
        _lastFinishedTimeMillis.value = now
        val context = getApplication<Application>().applicationContext
        context.stopService(Intent(context, StudyForegroundService::class.java))

        val minutes = (durationMillis / 60000).toInt()
        if (minutes >= 1) {
            _showCompletionDialog.value = true
        }
        _isStudying.value = false
        _startTimeMillis.value = null
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    fun startEditingLog(log: StudyLog) {
        _editingLog.value = log
    }

    fun cancelEditing() {
        _editingLog.value = null
    }

    fun updateLog(id: Int, minutes: Int) {
        viewModelScope.launch {
            repository.getLogById(id)?.let {
                repository.update(it.copy(durationMinutes = minutes))
            }
            _editingLog.value = null
        }
    }

    private fun formatDuration(ms: Long): String {
        val s = (ms / 1000) % 60
        val m = (ms / 60000) % 60
        val h = ms / 3600000
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
