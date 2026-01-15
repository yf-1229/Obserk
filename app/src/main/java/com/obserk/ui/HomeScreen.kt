package com.obserk.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    uiState.editingLog?.let { log ->
        EditLogDialog(
            log = log,
            onSave = { minutes -> viewModel.updateLog(log.id, minutes) },
            onCancel = { viewModel.cancelEditing() }
        )
    }

    val cardColors = if (uiState.isStudying) {
        CardDefaults.cardColors(containerColor = if (isDark) Color.Black else Color.White, contentColor = if (isDark) Color.White else Color.Black)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    }

    Card(
        modifier = modifier.fillMaxSize().padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.isStudying) 4.dp else 12.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = cardColors,
        onClick = {
            performHapticFeedback(context, isStudying = uiState.isStudying)
            viewModel.toggleStudying()
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Camera toggle button at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalIconButton(
                    onClick = { viewModel.toggleCamera() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isCameraEnabled) 
                            Icons.Default.Videocam 
                        else 
                            Icons.Default.VideocamOff,
                        contentDescription = if (uiState.isCameraEnabled)
                            stringResource(R.string.camera_on)
                        else
                            stringResource(R.string.camera_off),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Main content
            BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompletionDialog(labels: List<String>, onLabelSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var newLabel by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.congratulations)) },
        text = {
            Column {
                Text(stringResource(R.string.what_did_you_do))
                FlowRow(modifier = Modifier.padding(vertical = 8.dp)) {
                    labels.forEach { label ->
                        SuggestionChip(onClick = { onLabelSelected(label) }, label = { Text(label) }, modifier = Modifier.padding(4.dp))
                    }
                }
                OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text(stringResource(R.string.new_label)) })
            }
        },
        confirmButton = {
            Button(onClick = { if (newLabel.isNotBlank()) onLabelSelected(newLabel) }) { Text(stringResource(R.string.save)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditLogDialog(log: StudyLog, onSave: (Int) -> Unit, onCancel: () -> Unit) {
    var minutes by remember { mutableStateOf(log.durationMinutes.toString()) }
    var selectedLabel by remember { mutableStateOf(log.label ?: "") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.edit_record)) },
        text = {
            Column {
                OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text(stringResource(R.string.time_minutes)) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(minutes.toIntOrNull() ?: log.durationMinutes) }) { Text(stringResource(R.string.update)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } }
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
        val successPattern = longArrayOf(0, 40, 60, 40, 100, 40, 60, 150)
        vibrator.vibrate(VibrationEffect.createWaveform(successPattern, -1))
    }
}

@Composable
private fun DisplayContent(uiState: HomeUiState, isDark: Boolean) {
    if (uiState.isStudying) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val startTimeText = uiState.startTimeMillis?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: "--:--"
            Text(text = stringResource(R.string.started_at, startTimeText), style = MaterialTheme.typography.displayMedium)

            uiState.latestMlResult?.let { result ->
                Spacer(modifier = Modifier.height(24.dp))
                val isSuccess = result
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSuccess) stringResource(R.string.studying_detected) else stringResource(R.string.recording_interrupted),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isSuccess) {
                    Text(
                        text = stringResource(R.string.pen_not_detected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB71C1C)
                    )
                }
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Obserk", style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp))
            Text(text = stringResource(R.string.start_recording), style = MaterialTheme.typography.titleMedium, color = if (isDark) Color.Gray else Color.DarkGray)
        }
    }
}
