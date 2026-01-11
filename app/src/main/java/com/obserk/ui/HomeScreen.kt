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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.obserk.R

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
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (uiState.isStudying) {
                    stringResource(R.string.studying_label)
                } else {
                    stringResource(R.string.time_after_prestudy)
                },
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.Center)
            )

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
            
            // 経過時間を表示（ストップウォッチ）
            if (uiState.isStudying) {
                Text(
                    text = stringResource(R.string.minutes_unit, uiState.studyTimeMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                )
            }
        }
    }
}
