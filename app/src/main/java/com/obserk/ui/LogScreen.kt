package com.obserk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obserk.R

@Composable
fun LogScreen(uiState: HomeUiState, viewModel: HomeViewModel) {
    val dailyStats = uiState.logs
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
        .toList()
        .sortedByDescending { it.first }
        .take(7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.study_logs_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (dailyStats.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxMinutes = dailyStats.maxOf { it.second }.toFloat().coerceAtLeast(1f)
                    dailyStats.reversed().forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val barHeight = (stat.second / maxMinutes * 120).dp
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = stat.first.takeLast(5),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.logs) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.startEditingLog(log) }
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = log.date, style = MaterialTheme.typography.bodyLarge)
                            log.label?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.minutes_unit, log.durationMinutes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
