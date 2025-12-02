package com.habittracker.ui.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerCompletionInteractor.ConfirmType
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.resolveTimerUxEntryPoint
import com.habittracker.timing.TimerController
import com.habittracker.timing.TimerFeatureFlags
import com.habittracker.ui.components.timer.RadialTimer
import com.habittracker.ui.modifiers.disableDuringTimerAction
import com.habittracker.ui.utils.TimerActionEventEffect
import com.habittracker.ui.viewmodels.timing.ActiveTimerViewModel
import kotlinx.coroutines.launch

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
    val useCoordinator = TimerFeatureFlags.enableActionCoordinator
    val handler = remember(ctx, useCoordinator) {
        if (useCoordinator) resolveTimerUxEntryPoint(ctx).timerActionHandler() else null
    }
    val coordinatorState by if (handler != null) {
        handler.state.collectAsState()
    } else {
        remember(handler) { mutableStateOf(TimerActionCoordinator.CoordinatorState()) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var confirmBelowMin by remember { mutableStateOf<Int?>(null) }
    var confirmDiscard by remember { mutableStateOf<Int?>(null) }
    var confirmEndPomodoro by remember { mutableStateOf(false) }
    var confirmCompleteWithoutTimer by remember { mutableStateOf(false) }

    val controlsEnabled = handler == null || !coordinatorState.waitingForService
    val pausedForUi = if (handler != null) coordinatorState.paused else state.paused
    val controlModifier = if (handler != null) Modifier.disableDuringTimerAction(coordinatorState) else Modifier

    if (handler != null) {
        TimerActionEventEffect(
            handler = handler,
            onConfirm = { event ->
                if (event.habitId != state.habitId) return@TimerActionEventEffect
                when (event.type) {
                    ConfirmType.BelowMinDuration -> {
                        confirmBelowMin = (event.payload as? Int) ?: 0
                    }
                    ConfirmType.DiscardNonZeroSession -> {
                        confirmDiscard = (event.payload as? Int) ?: 0
                    }
                    ConfirmType.EndPomodoroEarly -> {
                        confirmEndPomodoro = true
                    }
                    ConfirmType.CompleteWithoutTimer -> {
                        confirmCompleteWithoutTimer = true
                    }
                }
            },
            onSnackbar = { message ->
                snackbarScope.launch { snackbarHostState.showSnackbar(message) }
            },
            onUndo = { message ->
                snackbarScope.launch { snackbarHostState.showSnackbar(message) }
            },
            onTip = { message ->
                snackbarScope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Session Controls", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            RadialTimer(
                totalMillis = state.totalMs,
                remainingMillis = state.remainingMs,
                isPaused = pausedForUi,
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        if (handler != null) {
                            val intent = if (pausedForUi) TimerIntent.Resume else TimerIntent.Pause
                            handler.handle(intent, state.habitId)
                        } else {
                            if (state.paused) controller.resume() else controller.pause()
                        }
                    },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(
                        imageVector = if (pausedForUi) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (pausedForUi) stringResource(id = com.habittracker.R.string.action_resume) else stringResource(id = com.habittracker.R.string.action_pause)
                    )
                }
                Button(
                    onClick = {
                        if (handler != null) {
                            handler.handle(TimerIntent.Done, state.habitId)
                        } else {
                            controller.complete()
                            onDismiss()
                        }
                    },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(id = com.habittracker.R.string.action_complete))
                }
            }
            Spacer(Modifier.height(8.dp))
            val minsLeft = (state.remainingMs / 1000) / 60
            if (state.autoComplete && !pausedForUi && minsLeft in 1..2) {
                Text(
                    text = stringResource(id = com.habittracker.R.string.nudge_near_target, minsLeft.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { controller.subtractOneMinute() },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(id = com.habittracker.R.string.action_sub_1m))
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(id = com.habittracker.R.string.action_sub_1m))
                }
                OutlinedButton(
                    onClick = { controller.addOneMinute() },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(id = com.habittracker.R.string.action_add_1m))
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(id = com.habittracker.R.string.action_add_1m))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onLogPartial != null) {
                    OutlinedButton(
                        onClick = {
                            onLogPartial(state.habitId)
                            onDismiss()
                        },
                        enabled = controlsEnabled,
                        modifier = controlModifier
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = stringResource(id = com.habittracker.R.string.log_partial))
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(id = com.habittracker.R.string.log_partial))
                    }
                }
                if (onAddNote != null) {
                    OutlinedButton(
                        onClick = { onAddNote(state.habitId) },
                        enabled = controlsEnabled,
                        modifier = controlModifier
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(id = com.habittracker.R.string.add_note))
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(id = com.habittracker.R.string.add_note))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SnackbarHost(hostState = snackbarHostState)
        }
    }
    if (confirmBelowMin != null) {
        val minSec = confirmBelowMin!!.coerceAtLeast(0)
        val minutes = (minSec / 60).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = { confirmBelowMin = null },
            title = { Text("Below minimum duration") },
            text = { Text("Below your minimum ${minutes}m. Complete anyway or keep timing?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBelowMin = null
                    if (handler != null) {
                        handler.handle(
                            TimerIntent.Done,
                            state.habitId,
                            TimerActionCoordinator.DecisionContext(
                                confirmation = TimerActionCoordinator.ConfirmationOverride.COMPLETE_BELOW_MINIMUM
                            )
                        )
                    } else {
                        controller.complete()
                    }
                    onDismiss()
                }) {
                    Text(text = stringResource(id = com.habittracker.R.string.action_complete))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onLogPartial != null) {
                        val logPartial = onLogPartial
                        TextButton(onClick = {
                            confirmBelowMin = null
                            if (handler != null) {
                                handler.handle(
                                    TimerIntent.StopWithoutComplete,
                                    state.habitId,
                                    TimerActionCoordinator.DecisionContext(
                                        confirmation = TimerActionCoordinator.ConfirmationOverride.LOG_PARTIAL_BELOW_MINIMUM
                                    )
                                )
                            }
                            logPartial(state.habitId)
                            onDismiss()
                        }) {
                            Text(text = stringResource(id = com.habittracker.R.string.log_partial))
                        }
                    }
                    TextButton(onClick = { confirmBelowMin = null }) {
                        Text("Keep timing")
                    }
                }
            }
        )
    }
    if (confirmDiscard != null) {
        val seconds = confirmDiscard!!.coerceAtLeast(0)
        val minutes = seconds / 60
        val discardLabel = if (minutes > 0) "${minutes}m" else "${seconds}s"
        AlertDialog(
            onDismissRequest = { confirmDiscard = null },
            title = { Text("Discard session?") },
            text = { Text("Discard $discardLabel session?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = null
                    if (handler != null) {
                        handler.handle(
                            TimerIntent.StopWithoutComplete,
                            state.habitId,
                            TimerActionCoordinator.DecisionContext(
                                confirmation = TimerActionCoordinator.ConfirmationOverride.DISCARD_SESSION
                            )
                        )
                    } else {
                        controller.stop()
                    }
                    onDismiss()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (confirmEndPomodoro) {
        AlertDialog(
            onDismissRequest = { confirmEndPomodoro = false },
            title = { Text("End focus early?") },
            text = { Text("End the current focus session and start break?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEndPomodoro = false
                    if (handler != null) {
                        handler.handle(
                            TimerIntent.Done,
                            state.habitId,
                            TimerActionCoordinator.DecisionContext(
                                confirmation = TimerActionCoordinator.ConfirmationOverride.END_POMODORO_EARLY
                            )
                        )
                    } else {
                        controller.complete()
                    }
                    onDismiss()
                }) {
                    Text("End session")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEndPomodoro = false }) {
                    Text("Keep timing")
                }
            }
        )
    }
    if (confirmCompleteWithoutTimer) {
        // Signal to coordinator that dialog is open
        androidx.compose.runtime.LaunchedEffect(Unit) {
            handler?.setPendingConfirmation(state.habitId, ConfirmType.CompleteWithoutTimer)
        }
        
        AlertDialog(
            onDismissRequest = {
                confirmCompleteWithoutTimer = false
                handler?.clearPendingConfirmation()
            },
            title = { Text("Complete without timer?") },
            text = { Text("You have a timer enabled. Complete without running it?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCompleteWithoutTimer = false
                    handler?.clearPendingConfirmation()
                    if (handler != null) {
                        handler.handle(
                            TimerIntent.Done,
                            state.habitId,
                            TimerActionCoordinator.DecisionContext(
                                confirmation = TimerActionCoordinator.ConfirmationOverride.COMPLETE_WITHOUT_TIMER
                            )
                        )
                    } else {
                        controller.complete()
                    }
                    onDismiss()
                }) {
                    Text("Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmCompleteWithoutTimer = false
                    handler?.clearPendingConfirmation()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}



