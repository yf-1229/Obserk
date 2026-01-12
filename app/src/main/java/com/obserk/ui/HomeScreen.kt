package com.obserk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obserk.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCardPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val elevationSize = if (uiState.isStudying) 4.dp else 12.dp

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
        // BoxWithConstraints を使ってアダプティブレイアウトを実現
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                // 横向き（Landscape）: 左右に要素を配置
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DisplayContent(uiState, isDark)
                    
                    Text(
                        text = if (uiState.isStudying) {
                            stringResource(R.string.tap_to_stop)
                        } else {
                            stringResource(R.string.tap_to_start)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                // 縦向き（Portrait）: 従来通り上下に配置
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DisplayContent(uiState, isDark)
                    }

                    Text(
                        text = if (uiState.isStudying) {
                            stringResource(R.string.tap_to_stop)
                        } else {
                            stringResource(R.string.tap_to_start)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayContent(uiState: HomeUiState, isDark: Boolean) {
    if (uiState.isStudying) {
        val startTimeText = uiState.startTimeMillis?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "--:--"
        Text(
            text = stringResource(R.string.started_at, startTimeText),
            style = MaterialTheme.typography.displayMedium
        )
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.since_last_time),
                style = MaterialTheme.typography.titleMedium,
                color = if (isDark) Color.Gray else Color.DarkGray
            )
            Text(
                text = uiState.timeSinceLastStudy,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
            )
        }
    }
}
