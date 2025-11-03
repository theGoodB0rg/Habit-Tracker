package com.habittracker.ui.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.habittracker.timerux.TimerActionCoordinator

fun Modifier.disableDuringTimerAction(state: TimerActionCoordinator.CoordinatorState): Modifier {
    return if (state.waitingForService) {
        this.alpha(0.4f)
    } else {
        this
    }
}
