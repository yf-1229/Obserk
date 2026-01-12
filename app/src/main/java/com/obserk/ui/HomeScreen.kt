package com.obserk.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obserk.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    if (uiState.showCompletionDialog) {
        CompletionDialog(
            labels = uiState.labels,
            onLabelSelected = { viewModel.addLabelToLastLog(it) },
            onDismiss = { viewModel.dismissCompletionDialog() }
        )
    }

    uiState.editingLog?.let { log ->
        EditLogDialog(
            log = log,
            labels = uiState.labels,
            onSave = { minutes, label -> viewModel.updateLog(log.id, minutes, label) },
            onCancel = { viewModel.cancelEditing() }
        )
    }

    val cardColors = if (uiState.isStudying) {
        CardDefaults.cardColors(containerColor = if (isDark) Color.Black else Color.White, contentColor = if (isDark) Color.White else Color.Black)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    }

    Card(
        modifier = modifier.fillMaxSize().padding(bottom = 8.dp).safeDrawingPadding(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.isStudying) 4.dp else 12.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = cardColors,
        onClick = {
            performHapticFeedback(context, isStudying = uiState.isStudying)
            viewModel.toggleStudying()
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    DisplayContent(uiState, isDark)
                    Text(text = if (uiState.isStudying) stringResource(R.string.tap_to_stop) else stringResource(R.string.tap_to_start), style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { DisplayContent(uiState, isDark) }
                    Text(text = if (uiState.isStudying) stringResource(R.string.tap_to_stop) else stringResource(R.string.tap_to_start), style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompletionDialog(labels: List<String>, onLabelSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var newLabel by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("おめでとう！") },
        text = {
            Column {
                Text("何をしましたか？")
                FlowRow(modifier = Modifier.padding(vertical = 8.dp)) {
                    labels.forEach { label ->
                        SuggestionChip(onClick = { onLabelSelected(label) }, label = { Text(label) }, modifier = Modifier.padding(4.dp))
                    }
                }
                OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text("新しいラベル") })
            }
        },
        confirmButton = {
            Button(onClick = { if (newLabel.isNotBlank()) onLabelSelected(newLabel) }) { Text("保存") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditLogDialog(log: StudyLog, labels: List<String>, onSave: (Int, String?) -> Unit, onCancel: () -> Unit) {
    var minutes by remember { mutableStateOf(log.durationMinutes.toString()) }
    var selectedLabel by remember { mutableStateOf(log.label ?: "") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("記録の編集") },
        text = {
            Column {
                OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("時間 (分)") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("ラベル")
                FlowRow(modifier = Modifier.padding(vertical = 8.dp)) {
                    labels.forEach { label ->
                        FilterChip(selected = selectedLabel == label, onClick = { selectedLabel = label }, label = { Text(label) }, modifier = Modifier.padding(4.dp))
                    }
                }
                OutlinedTextField(value = selectedLabel, onValueChange = { selectedLabel = it }, label = { Text("新しいラベル") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(minutes.toIntOrNull() ?: log.durationMinutes, if (selectedLabel.isBlank()) null else selectedLabel) }) { Text("更新") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("キャンセル") } }
    )
}

private fun performHapticFeedback(context: Context, isStudying: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    }
    if (!vibrator.hasVibrator()) return
    if (!isStudying) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1))
    } else {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50, 100, 50, 200, 300, 100, 300), -1))
    }
}

@Composable
private fun DisplayContent(uiState: HomeUiState, isDark: Boolean) {
    if (uiState.isStudying) {
        val startTimeText = uiState.startTimeMillis?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: "--:--"
        Text(text = stringResource(R.string.started_at, startTimeText), style = MaterialTheme.typography.displayMedium)
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.since_last_time), style = MaterialTheme.typography.titleMedium, color = if (isDark) Color.Gray else Color.DarkGray)
            Text(text = uiState.timeSinceLastStudy, style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp))
        }
    }
}
