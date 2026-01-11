package com.obserk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCardPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    
    // isStudying に基づいて Elevation を調整
    val elevationSize = if (uiState.isStudying) 4.dp else 12.dp

    // isStudying に基づいてカラーを設定
    val cardColors = if (uiState.isStudying) {
        CardDefaults.cardColors(
            containerColor = if (isDark) Color.Black else Color.White,
            contentColor = if (isDark) Color.White else Color.Black
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
            .safeDrawingPadding(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = elevationSize),
        border = CardDefaults.outlinedCardBorder(),
        colors = cardColors,
        onClick = { onCardPressed() },
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 中央のテキスト
            Text(
                text = if (uiState.isStudying) "Studying..." else "Time after preStudy",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.Center)
            )

            // 下部のテキスト
            Text(
                text = if (uiState.isStudying) "Tap to Stop" else "Tap to Start",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(uiState = HomeUiState(), onCardPressed = {})
}
