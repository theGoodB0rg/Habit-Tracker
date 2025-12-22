package com.habittracker.timerux

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1: Central interactor turning intents into outcomes.
 * Keeps UI dumb and consistent across surfaces.
 */
@Singleton
class TimerCompletionInteractor @Inject constructor() {

    // Inputs for decision making
    data class Inputs(
        val habitId: Long,
        val timerEnabled: Boolean,
        val requireTimerToComplete: Boolean,
        val minDurationSec: Int?,
        val targetDurationSec: Int?,
        val timerState: TimerState,
        val elapsedSec: Int,
        val todayCompleted: Boolean,
        val platform: Platform = Platform.APP,
        val singleActiveTimer: Boolean = true,
        // Phase 5 — Pomodoro inputs
        val timerType: com.habittracker.ui.models.timing.TimerType? = null,
        val isInBreak: Boolean = false,
        val logPartial: Boolean = false,

        val requestedDurationSec: Int? = null,
        val askToCompleteWithoutTimer: Boolean = true
    )

    enum class Platform { APP, WIDGET, NOTIF }
    enum class TimerState { IDLE, RUNNING, PAUSED }

    sealed interface Intent { object Start: Intent; object Pause: Intent; object Resume: Intent; object Done: Intent; object StopWithoutComplete: Intent; object QuickComplete: Intent }

    sealed interface ActionOutcome {
        data class Execute(val actions: List<Action>, val undoable: Boolean = true) : ActionOutcome
        data class Confirm(val type: ConfirmType, val payload: Any? = null) : ActionOutcome
        data class Disallow(val message: String) : ActionOutcome
    }

    sealed interface Action {
        data class StartTimer(val habitId: Long, val durationOverrideSec: Int? = null): Action
        data class PauseTimer(val habitId: Long): Action
        data class ResumeTimer(val habitId: Long): Action
        /**
         * persistDirectly=true means write completion immediately (no timer event expected).
         * persistDirectly=false means rely on the timer service Completed event to persist.
         */
        data class CompleteToday(
            val habitId: Long,
            val logDuration: Boolean,
            val partial: Boolean = false,
            val persistDirectly: Boolean = false
        ): Action
        data class SavePartial(val habitId: Long, val durationSec: Int): Action
        data class DiscardSession(val habitId: Long): Action
        data class ShowUndo(val message: String): Action
        data class ShowTip(val message: String): Action
    }

    enum class ConfirmType { BelowMinDuration, DiscardNonZeroSession, EndPomodoroEarly, CompleteWithoutTimer }

    fun decide(intent: Intent, inputs: Inputs): ActionOutcome {
        // Heuristics
        val smallProgress = inputs.elapsedSec < 120 || (inputs.targetDurationSec?.let { inputs.elapsedSec < (it * 0.2).toInt() } ?: false)

        return when (intent) {
            Intent.QuickComplete -> {
                if (inputs.requireTimerToComplete && inputs.timerEnabled) {
                    return ActionOutcome.Disallow("This habit requires using the timer. Tap the timer button to start.")
                }
                ActionOutcome.Execute(
                    actions = listOf(
                        Action.CompleteToday(inputs.habitId, logDuration = false, persistDirectly = true),
                        Action.ShowUndo("Marked as done. Undo")
                    ),
                    undoable = true
                )
            }
            Intent.Start -> {
                val overrideSec = inputs.requestedDurationSec?.takeIf { it > 0 }
                ActionOutcome.Execute(listOf(Action.StartTimer(inputs.habitId, overrideSec)))
            }
            Intent.Pause -> ActionOutcome.Execute(listOf(Action.PauseTimer(inputs.habitId)))
            Intent.Resume -> ActionOutcome.Execute(listOf(Action.ResumeTimer(inputs.habitId)))
            Intent.StopWithoutComplete -> {
                if (inputs.logPartial) {
                    val duration = inputs.elapsedSec.coerceAtLeast(0)
                    return ActionOutcome.Execute(
                        actions = listOf(
                            Action.SavePartial(inputs.habitId, duration),
                            Action.ShowUndo("Logged as partial. Undo")
                        )
                    )
                }
                if (inputs.elapsedSec > 0) ActionOutcome.Confirm(ConfirmType.DiscardNonZeroSession, inputs.elapsedSec)
                else ActionOutcome.Execute(listOf(Action.DiscardSession(inputs.habitId)))
            }
            Intent.Done -> {
                if (!inputs.timerEnabled || inputs.timerState == TimerState.IDLE) {
                    if (inputs.requireTimerToComplete && inputs.timerEnabled) {
                        return ActionOutcome.Disallow("This habit requires using the timer. Tap the timer button to start.")
                    }
                    if (inputs.timerEnabled && inputs.askToCompleteWithoutTimer) {
                        return ActionOutcome.Confirm(ConfirmType.CompleteWithoutTimer)
                    }
                    return ActionOutcome.Execute(
                        actions = listOf(
                            Action.CompleteToday(inputs.habitId, logDuration = false, persistDirectly = true),
                            Action.ShowUndo("Completed without timing. Undo")
                        ),
                        undoable = true
                    )
                }

                // Timer is running or paused
                inputs.minDurationSec?.let { min ->
                    if (inputs.elapsedSec < min) {
                        return ActionOutcome.Confirm(ConfirmType.BelowMinDuration, min)
                    }
                }
                // Phase 5 — If Pomodoro focus segment is ended early, ask for confirmation
                if (inputs.timerType == com.habittracker.ui.models.timing.TimerType.POMODORO
                    && inputs.timerState == TimerState.RUNNING
                    && !inputs.isInBreak
                    && (inputs.targetDurationSec != null && inputs.elapsedSec < inputs.targetDurationSec)) {
                    return ActionOutcome.Confirm(ConfirmType.EndPomodoroEarly, inputs.elapsedSec)
                }
                val actions = mutableListOf<Action>(
                    // Timer path: rely on service Completed event to persist
                    Action.CompleteToday(inputs.habitId, logDuration = true, persistDirectly = false),
                    Action.ShowUndo("Marked as done. Undo")
                )
                if (smallProgress) {
                    // Optionally show a first-time tip; keep as action for now
                    actions.add(Action.ShowTip("You can time this habit."))
                }
                ActionOutcome.Execute(actions)
            }
        }
    }
}
