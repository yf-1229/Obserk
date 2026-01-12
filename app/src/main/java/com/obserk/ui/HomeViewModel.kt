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
        repository.allLogs
    ) { isStudying, startTime, lastFinished, currentTime, dbLogs ->
        
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
        val now = System.currentTimeMillis()
        _isStudying.value = true
        _startTimeMillis.value = now
        
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
        // 1分以上の場合のみ記録する
        if (minutes >= 1) {
            saveLog(minutes)
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

    private fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
