package com.obserk.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.obserk.data.AppDatabase
import com.obserk.data.StudyLabelEntity
import com.obserk.data.StudyLogEntity
import com.obserk.data.StudyLogRepository
import com.obserk.service.StudyForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudyLogRepository
    private val _isStudying = MutableStateFlow(false)
    private val _startTimeMillis = MutableStateFlow<Long?>(null)
    private val _lastFinishedTimeMillis = MutableStateFlow<Long?>(null)
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    
    private val _showCompletionDialog = MutableStateFlow(false)
    private val _editingLog = MutableStateFlow<StudyLog?>(null)
    
    private var timerJob: Job? = null

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
        _isStudying,
        _startTimeMillis,
        _lastFinishedTimeMillis,
        _currentTimeMillis,
        repository.allLogs,
        repository.allLabels,
        _showCompletionDialog,
        _editingLog
    ) { params ->
        HomeUiState(
            isStudying = params[0] as Boolean,
            startTimeMillis = params[1] as Long?,
            lastFinishedTimeMillis = params[2] as Long?,
            timeSinceLastStudy = if (!(params[0] as Boolean) && params[2] != null) formatDuration((params[3] as Long) - (params[2] as Long)) else "00:00:00",
            logs = (params[4] as List<StudyLogEntity>).map { StudyLog(it.id, it.date, it.durationMinutes, it.label) },
            labels = (params[5] as List<StudyLabelEntity>).map { it.name },
            showCompletionDialog = params[6] as Boolean,
            editingLog = params[7] as StudyLog?
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }

    private fun stopStopwatch() {
        val now = System.currentTimeMillis()
        val durationMillis = _startTimeMillis.value?.let { now - it } ?: 0L
        _lastFinishedTimeMillis.value = now
        val context = getApplication<Application>().applicationContext
        context.stopService(Intent(context, StudyForegroundService::class.java))
        val minutes = (durationMillis / 60000).toInt()
        if (minutes >= 1) {
            saveLog(minutes)
            _showCompletionDialog.value = true
        }
        _isStudying.value = false
        _startTimeMillis.value = null
    }

    private fun saveLog(minutes: Int) {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            repository.insert(StudyLogEntity(date = date, durationMinutes = minutes))
        }
    }

    fun dismissCompletionDialog() { _showCompletionDialog.value = false }

    fun addLabelToLastLog(label: String) {
        viewModelScope.launch {
            repository.getLatestLog()?.let { repository.update(it.copy(label = label)) }
            repository.insertLabel(StudyLabelEntity(label))
            _showCompletionDialog.value = false
        }
    }

    fun startEditingLog(log: StudyLog) { _editingLog.value = log }
    fun cancelEditing() { _editingLog.value = null }

    fun updateLog(id: Int, minutes: Int, label: String?) {
        viewModelScope.launch {
            repository.getLogById(id)?.let { repository.update(it.copy(durationMinutes = minutes, label = label)) }
            if (label != null) repository.insertLabel(StudyLabelEntity(label))
            _editingLog.value = null
        }
    }

    private fun formatDuration(ms: Long): String {
        val s = (ms / 1000) % 60; val m = (ms / 60000) % 60; val h = ms / 3600000
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
