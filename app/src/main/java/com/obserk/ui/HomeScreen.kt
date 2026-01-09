package com.obserk.ui

import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(

) {
    val uiState by viewModel.uiState.collectAsState()
}