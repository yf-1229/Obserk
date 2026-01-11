package com.obserk.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var userActive by mutableStateOf(false)
    private set

    init {
        // Initialize or load data here
    }

    fun resetState() {
        _uiState.value = HomeUiState()
    }

    fun toggleStudying() {
        _uiState.value = _uiState.value.copy(isStudying = !_uiState.value.isStudying)
    }
    fun updateStudyTime(minutes: Int) {
        _uiState.value = _uiState.value.copy(studyTimeMinutes = minutes)
    }

    fun updateHomeState(studyTime: Int) {

    }


}