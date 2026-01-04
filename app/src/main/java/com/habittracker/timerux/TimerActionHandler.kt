package com.habittracker.timerux

import com.habittracker.di.MainDispatcher
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerActionHandler @Inject constructor(
    private val coordinator: TimerActionCoordinator,
    @MainDispatcher private val mainScope: CoroutineScope
) {

    val state get() = coordinator.state
    val events get() = coordinator.events
    val telemetry get() = coordinator.telemetry

    fun handle(
        intent: TimerIntent,
        habitId: Long,
        context: TimerActionCoordinator.DecisionContext = TimerActionCoordinator.DecisionContext()
    ) {
        mainScope.launch {
            val decision = coordinator.decide(intent, habitId, context)
            coordinator.handleOutcome(decision, habitId)
        }
    }

    /**
     * Signal that a confirmation dialog is being shown for a habit.
     * Delegates to coordinator.
     */
    fun setPendingConfirmation(habitId: Long, type: TimerCompletionInteractor.ConfirmType) {
        coordinator.setPendingConfirmation(habitId, type)
    }

    /**
     * Clear the pending confirmation state.
     * Delegates to coordinator.
     */
    fun clearPendingConfirmation() {
        coordinator.clearPendingConfirmation()
    }
    
    /**
     * Clear the error state in the coordinator.
     */
    fun clearError() {
        coordinator.clearError()
    }
    
    /**
     * Clear the paused habit state (used when user dismisses timer switcher).
     */
    fun clearPausedHabit() {
        coordinator.clearPausedHabit()
    }
}

