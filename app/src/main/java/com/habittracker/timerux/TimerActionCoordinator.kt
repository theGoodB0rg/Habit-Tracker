package com.habittracker.timerux

import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.di.ApplicationScope
import com.habittracker.timing.TimerBus
import com.habittracker.timing.TimerController
import com.habittracker.timing.TimerEvent
import com.habittracker.timerux.TimerCompletionInteractor.Action as TimerAction
import com.habittracker.timerux.TimerCompletionInteractor.ActionOutcome as TimerOutcome
import com.habittracker.timerux.TimerCompletionInteractor.ConfirmType
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.TimerCompletionInteractor.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerActionCoordinator @Inject constructor(
    private val interactor: TimerCompletionInteractor,
    private val timingRepository: TimingRepository,
    private val timerController: TimerController,
    @ApplicationScope private val appScope: CoroutineScope
) {

    data class CoordinatorState(
        val trackedHabitId: Long? = null,
        val timerState: TimerState = TimerState.IDLE,
        val remainingMs: Long = 0L,
        val paused: Boolean = false,
        val waitingForService: Boolean = false,
        val lastOutcome: TimerOutcome? = null
    )

    data class DecisionContext(
        val platform: TimerCompletionInteractor.Platform = TimerCompletionInteractor.Platform.APP,
        val smartDuration: Duration? = null,
        val confirmation: ConfirmationOverride? = null
    )

    enum class ConfirmationOverride {
        COMPLETE_BELOW_MINIMUM,
        DISCARD_SESSION,
        END_POMODORO_EARLY
    }

    sealed interface UiEvent {
        data class Snackbar(val message: String) : UiEvent
        data class Undo(val message: String) : UiEvent
        data class Tip(val message: String) : UiEvent
        data class Confirm(val habitId: Long, val type: ConfirmType, val payload: Any?) : UiEvent
    }

    sealed interface TimerActionTelemetry {
        val habitId: Long
        val intent: TimerIntent

        data class Executed(override val habitId: Long, override val intent: TimerIntent) : TimerActionTelemetry
        data class Confirmed(
            override val habitId: Long,
            override val intent: TimerIntent,
            val confirmType: ConfirmType
        ) : TimerActionTelemetry
        data class Disallowed(
            override val habitId: Long,
            override val intent: TimerIntent,
            val reason: String
        ) : TimerActionTelemetry
    }

    private val _state = MutableStateFlow(CoordinatorState())
    val state: StateFlow<CoordinatorState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _telemetry = MutableSharedFlow<TimerActionTelemetry>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val telemetry: SharedFlow<TimerActionTelemetry> = _telemetry.asSharedFlow()

    private val remainingByHabit = mutableMapOf<Long, Long>()
    private val pausedByHabit = mutableMapOf<Long, Boolean>()

    private var lastActionAt: Long = 0L
    private var inFlightIntent: TimerIntent? = null
    private val debounceMs = 500L

    init {
        appScope.launch {
            TimerBus.events.collect(::onTimerEvent)
        }
    }

    suspend fun decide(
        intent: TimerIntent,
        habitId: Long,
        context: DecisionContext = DecisionContext()
    ): TimerOutcome {
        val now = System.currentTimeMillis()
        if (shouldDebounce(intent, now)) {
            return TimerOutcome.Disallow("Action already in flight")
        }

        val session = timingRepository.getActiveTimerSession(habitId)
        val timing = timingRepository.getHabitTiming(habitId)
        val remaining = remainingByHabit[habitId] ?: session?.remainingTime?.toMillis() ?: 0L
        val isPaused = pausedByHabit[habitId] ?: (session?.isPaused == true)
        val isRunning = remaining > 0 && !isPaused

        val redirectedIntent = when {
            intent == TimerIntent.Start && (isRunning || session?.isRunning == true) -> TimerIntent.Resume
            intent == TimerIntent.Start && session?.isPaused == true -> TimerIntent.Resume
            else -> intent
        }

        val timerState = when {
            session?.isRunning == true -> TimerState.RUNNING
            session?.isPaused == true -> TimerState.PAUSED
            else -> TimerState.IDLE
        }

        val inputs = TimerCompletionInteractor.Inputs(
            habitId = habitId,
            timerEnabled = timing?.timerEnabled == true,
            requireTimerToComplete = timing?.requireTimerToComplete == true,
            minDurationSec = timing?.minDuration?.seconds?.toInt(),
            targetDurationSec = timing?.estimatedDuration?.seconds?.toInt(),
            timerState = timerState,
            elapsedSec = session?.elapsedTime?.seconds?.toInt() ?: 0,
            todayCompleted = false,
            platform = context.platform,
            singleActiveTimer = true,
            timerType = session?.type,
            isInBreak = session?.isInBreak == true
        )

        val adjustedInputs = when (context.confirmation) {
            ConfirmationOverride.COMPLETE_BELOW_MINIMUM -> {
                if (redirectedIntent == TimerIntent.Done) {
                    inputs.copy(minDurationSec = null)
                } else {
                    inputs
                }
            }
            ConfirmationOverride.DISCARD_SESSION -> {
                if (redirectedIntent == TimerIntent.StopWithoutComplete) {
                    inputs.copy(elapsedSec = 0)
                } else {
                    inputs
                }
            }
            ConfirmationOverride.END_POMODORO_EARLY -> {
                if (redirectedIntent == TimerIntent.Done) {
                    inputs.copy(
                        timerType = null,
                        isInBreak = true
                    )
                } else {
                    inputs
                }
            }
            null -> inputs
        }

        val outcome = interactor.decide(redirectedIntent, adjustedInputs)
        _state.value = when (outcome) {
            is TimerOutcome.Execute -> {
                lastActionAt = now
                inFlightIntent = redirectedIntent
                _state.value.copy(
                    trackedHabitId = habitId,
                    waitingForService = true,
                    lastOutcome = outcome,
                    timerState = timerState,
                    remainingMs = remaining,
                    paused = isPaused
                )
            }
            else -> _state.value.copy(lastOutcome = outcome)
        }

        if (outcome is TimerOutcome.Execute) {
            dispatchActions(outcome.actions, habitId)
        }

        return outcome
    }

    fun handleOutcome(intent: TimerIntent, outcome: TimerOutcome, habitId: Long) {
        when (outcome) {
            is TimerOutcome.Execute -> {
                emitTelemetry(TimerActionTelemetry.Executed(habitId, intent))
            }
            is TimerOutcome.Confirm -> {
                emitUiEvent(UiEvent.Confirm(habitId, outcome.type, outcome.payload))
                emitTelemetry(TimerActionTelemetry.Confirmed(habitId, intent, outcome.type))
            }
            is TimerOutcome.Disallow -> {
                emitUiEvent(UiEvent.Snackbar(outcome.message))
                emitTelemetry(TimerActionTelemetry.Disallowed(habitId, intent, outcome.message))
            }
        }
    }

    private fun shouldDebounce(intent: TimerIntent, now: Long): Boolean {
        if (intent !in debouncedIntents) return false
        if (_state.value.waitingForService) return true
        return (now - lastActionAt) < debounceMs
    }

    private fun dispatchActions(actions: List<TimerAction>, habitId: Long) {
        actions.forEach { action ->
            when (action) {
                is TimerAction.StartTimer -> timerController.start(action.habitId)
                is TimerAction.PauseTimer -> timerController.pause()
                is TimerAction.ResumeTimer -> timerController.resume()
                is TimerAction.CompleteToday -> timerController.complete()
                is TimerAction.SavePartial -> timerController.stop()
                is TimerAction.DiscardSession -> timerController.stop()
                is TimerAction.ShowUndo -> emitUiEvent(UiEvent.Undo(action.message))
                is TimerAction.ShowTip -> emitUiEvent(UiEvent.Tip(action.message))
            }
        }
    }

    private fun onTimerEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.Started -> {
                remainingByHabit[event.habitId] = event.targetMs
                pausedByHabit[event.habitId] = false
                updateTrackedState(
                    habitId = event.habitId,
                    waiting = false,
                    timerState = TimerState.RUNNING,
                    remaining = event.targetMs,
                    paused = false
                )
            }
            is TimerEvent.Tick -> {
                remainingByHabit[event.habitId] = event.remainingMs
                if (_state.value.trackedHabitId == event.habitId) {
                    _state.value = _state.value.copy(remainingMs = event.remainingMs)
                }
            }
            is TimerEvent.Paused -> {
                pausedByHabit[event.habitId] = true
                updateTrackedState(
                    habitId = event.habitId,
                    waiting = false,
                    timerState = TimerState.PAUSED,
                    paused = true
                )
            }
            is TimerEvent.Resumed -> {
                pausedByHabit[event.habitId] = false
                updateTrackedState(
                    habitId = event.habitId,
                    waiting = false,
                    timerState = TimerState.RUNNING,
                    paused = false
                )
            }
            is TimerEvent.Completed -> {
                remainingByHabit.remove(event.habitId)
                pausedByHabit.remove(event.habitId)
                markCompleted(event.habitId)
            }
            is TimerEvent.Error -> {
                if (_state.value.trackedHabitId == event.habitId) {
                    _state.value = _state.value.copy(waitingForService = false)
                }
                emitUiEvent(UiEvent.Snackbar(event.message))
            }
            is TimerEvent.Extended -> {
                remainingByHabit[event.habitId] = event.newTargetMs
                if (_state.value.trackedHabitId == event.habitId) {
                    _state.value = _state.value.copy(remainingMs = event.newTargetMs)
                }
            }
            else -> Unit
        }
    }

    private fun updateTrackedState(
        habitId: Long,
        waiting: Boolean,
        timerState: TimerState,
        remaining: Long? = null,
        paused: Boolean
    ) {
        if (_state.value.trackedHabitId == habitId) {
            _state.value = _state.value.copy(
                waitingForService = waiting,
                timerState = timerState,
                remainingMs = remaining ?: _state.value.remainingMs,
                paused = paused
            )
            if (!waiting) {
                inFlightIntent = null
            }
        }
    }

    private fun markCompleted(habitId: Long) {
        if (_state.value.trackedHabitId == habitId) {
            _state.value = _state.value.copy(
                waitingForService = false,
                trackedHabitId = null,
                timerState = TimerState.IDLE,
                remainingMs = 0L,
                paused = false
            )
            inFlightIntent = null
        }
    }

    private fun emitUiEvent(event: UiEvent) {
        _events.tryEmit(event)
    }

    private fun emitTelemetry(event: TimerActionTelemetry) {
        _telemetry.tryEmit(event)
    }

    companion object {
        private val debouncedIntents = setOf(
            TimerIntent.Start,
            TimerIntent.Resume,
            TimerIntent.Pause,
            TimerIntent.Done
        )
    }
}




