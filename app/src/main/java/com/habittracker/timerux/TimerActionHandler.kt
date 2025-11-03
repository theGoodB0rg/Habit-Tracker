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
            val outcome = coordinator.decide(intent, habitId, context)
            coordinator.handleOutcome(intent, outcome, habitId)
        }
    }
}

