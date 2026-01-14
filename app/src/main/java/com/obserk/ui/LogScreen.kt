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
    // グラフ用: 直近7回の効率性を表示
    val efficiencyStats = uiState.logs.take(7).reversed()

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

        // 効率性グラフセクション (Step 4)
        if (efficiencyStats.isNotEmpty()) {
            Text(text = "Efficiency (%)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    efficiencyStats.forEach { log ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val barHeight = (log.efficiency.coerceIn(0f, 100f) * 1.2f).dp
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "${log.efficiency.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp
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
                            Text(
                                text = "Eff: ${log.efficiency.toInt()}% (${log.durationMinutes}/${log.totalElapsedMinutes} min)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
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
