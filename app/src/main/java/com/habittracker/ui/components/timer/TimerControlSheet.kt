package com.habittracker.ui.components.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.habittracker.ui.components.timer.RadialTimer
import com.habittracker.ui.viewmodels.timing.ActiveTimerViewModel
import com.habittracker.timing.TimerController
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerControlSheet(
    onDismiss: () -> Unit = {},
    onLogPartial: ((habitId: Long) -> Unit)? = null,
    onAddNote: ((habitId: Long) -> Unit)? = null,
) {
    val vm: ActiveTimerViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val controller = remember(ctx) { TimerController(ctx) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Session Controls", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            // Progress ring
            RadialTimer(
                totalMillis = state.totalMs,
                remainingMillis = state.remainingMs,
                isPaused = state.paused,
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(16.dp))
            // Primary controls row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { if (state.paused) controller.resume() else controller.pause() }) {
                    Icon(imageVector = if (state.paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(text = if (state.paused) stringResource(id = com.habittracker.R.string.action_resume) else stringResource(id = com.habittracker.R.string.action_pause))
                }
                Button(onClick = { controller.complete(); onDismiss() }) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(id = com.habittracker.R.string.action_complete))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Gentle near-target nudge (~1-2m remaining) only when auto-complete is enabled
            val minsLeft = (state.remainingMs / 1000) / 60
            if (state.autoComplete && !state.paused && minsLeft in 1..2) {
                Text(
                    text = stringResource(id = com.habittracker.R.string.nudge_near_target, minsLeft.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }
            // +/- 1 minute controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { controller.subtractOneMinute() }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(id = com.habittracker.R.string.action_sub_1m))
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(id = com.habittracker.R.string.action_sub_1m))
                }
                OutlinedButton(onClick = { controller.addOneMinute() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = com.habittracker.R.string.action_add_1m))
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(id = com.habittracker.R.string.action_add_1m))
                }
            }
            Spacer(Modifier.height(12.dp))
            // Secondary actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onLogPartial != null) {
                    OutlinedButton(onClick = {
                        onLogPartial(state.habitId)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(id = com.habittracker.R.string.log_partial))
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(id = com.habittracker.R.string.log_partial))
                    }
                }
                if (onAddNote != null) {
                    OutlinedButton(onClick = { onAddNote(state.habitId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(id = com.habittracker.R.string.add_note))
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(id = com.habittracker.R.string.add_note))
                    }
                }
            }
        }
    }
}
