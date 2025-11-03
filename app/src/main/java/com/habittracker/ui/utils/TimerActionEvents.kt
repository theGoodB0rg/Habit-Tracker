package com.habittracker.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerActionHandler

@Composable
fun TimerActionEventEffect(
    handler: TimerActionHandler?,
    onConfirm: (TimerActionCoordinator.UiEvent.Confirm) -> Unit = {},
    onSnackbar: (String) -> Unit = {},
    onUndo: (String) -> Unit = {},
    onTip: (String) -> Unit = {}
) {
    if (handler == null) return

    LaunchedEffect(handler) {
        handler.events.collect { event ->
            when (event) {
                is TimerActionCoordinator.UiEvent.Confirm -> onConfirm(event)
                is TimerActionCoordinator.UiEvent.Snackbar -> onSnackbar(event.message)
                is TimerActionCoordinator.UiEvent.Undo -> onUndo(event.message)
                is TimerActionCoordinator.UiEvent.Tip -> onTip(event.message)
            }
        }
    }
}

@Composable
fun TimerActionTelemetryEffect(
    handler: TimerActionHandler?,
    onTelemetry: suspend (TimerActionCoordinator.TimerActionTelemetry) -> Unit
) {
    if (handler == null) return

    LaunchedEffect(handler) {
        handler.telemetry.collect { event ->
            onTelemetry(event)
        }
    }
}
