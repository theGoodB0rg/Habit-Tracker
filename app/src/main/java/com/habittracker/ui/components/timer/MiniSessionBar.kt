package com.habittracker.ui.components.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.resolveTimerUxEntryPoint
import com.habittracker.timing.TimerController
import com.habittracker.timing.TimerFeatureFlags
import com.habittracker.ui.modifiers.disableDuringTimerAction

@Composable
fun MiniSessionBar(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val controller = remember(ctx) { TimerController(ctx) }
    val useCoordinator = TimerFeatureFlags.enableActionCoordinator
    val handler = remember(ctx, useCoordinator) {
        if (useCoordinator) resolveTimerUxEntryPoint(ctx).timerActionHandler() else null
    }
    val coordinatorState by if (handler != null) {
        handler.state.collectAsState()
    } else {
        remember(handler) { mutableStateOf(TimerActionCoordinator.CoordinatorState()) }
    }
    val controlsEnabled = handler == null || !coordinatorState.isLoading
    val habitId = coordinatorState.trackedHabitId ?: 0L
    val active = coordinatorState.timerState != com.habittracker.timerux.TimerCompletionInteractor.TimerState.IDLE
    val pausedForUi = coordinatorState.paused
    val controlModifier = if (handler != null) Modifier.disableDuringTimerAction(coordinatorState) else Modifier
    var showSheet by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = active,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(12.dp)
                .semantics { contentDescription = "Active timer session controls" }
        ) {
            if (showSheet) {
                TimerControlSheet { showSheet = false }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalSeconds = (coordinatorState.remainingMs / 1000)
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val timeText = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }

                Column(Modifier.weight(1f)) {
                    Text(timeText, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { if (coordinatorState.targetMs == 0L) 0f else 1f - (coordinatorState.remainingMs / coordinatorState.targetMs.toFloat()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (handler != null) {
                            val intent = if (pausedForUi) TimerIntent.Resume else TimerIntent.Pause
                            handler.handle(intent, habitId)
                        } else {
                            if (coordinatorState.paused) controller.resume() else controller.pause()
                        }
                    },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(
                        imageVector = if (pausedForUi) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (pausedForUi) "Resume" else "Pause"
                    )
                }
                IconButton(
                    onClick = { controller.extendFiveMinutes() },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Extend 5 minutes")
                }
                IconButton(
                    onClick = {
                        if (handler != null) {
                            handler.handle(TimerIntent.Done, habitId)
                        } else {
                            controller.complete()
                        }
                    },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Complete session")
                }
                TextButton(onClick = { showSheet = true }) { Text("More") }
            }
        }
    }
}



