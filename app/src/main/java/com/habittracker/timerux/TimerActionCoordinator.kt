package com.habittracker.timerux

import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.data.preferences.TimingPreferencesRepository
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerActionCoordinator @Inject constructor(
    private val interactor: TimerCompletionInteractor,
    private val timingRepository: TimingRepository,
    private val timingPreferencesRepository: TimingPreferencesRepository,
    private val timerController: TimerController,
    @ApplicationScope private val appScope: CoroutineScope
) {

    data class CoordinatorState(
        val trackedHabitId: Long? = null,
        val timerState: TimerState = TimerState.IDLE,
        val remainingMs: Long = 0L,
        val paused: Boolean = false,
        val waitingForService: Boolean = false,
        val lastOutcome: TimerOutcome? = null,
        val pendingConfirmHabitId: Long? = null,  // Habit with open confirmation dialog
        val pendingConfirmType: ConfirmType? = null  // Type of confirmation being shown
    )

    data class DecisionContext(
        val platform: TimerCompletionInteractor.Platform = TimerCompletionInteractor.Platform.APP,
        val smartDuration: Duration? = null,
        val confirmation: ConfirmationOverride? = null
    )

    enum class ConfirmationOverride {
        COMPLETE_BELOW_MINIMUM,
        DISCARD_SESSION,
        END_POMODORO_EARLY,

        LOG_PARTIAL_BELOW_MINIMUM,
        COMPLETE_WITHOUT_TIMER
    }

    sealed interface UiEvent {
        data class Snackbar(val message: String) : UiEvent
        data class Undo(val message: String) : UiEvent
        data class Tip(val message: String) : UiEvent
        data class Confirm(val habitId: Long, val type: ConfirmType, val payload: Any?) : UiEvent
        data class Completed(val habitId: Long) : UiEvent
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
    private var inFlightHabitId: Long? = null
    private var reservedIntent: TimerIntent? = null
    private var reservedHabitId: Long? = null
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
    ): TimerActionDecision {
        val now = System.currentTimeMillis()
        if (!tryReserveIntent(intent, habitId, now)) {
            return TimerActionDecision(
                resolvedIntent = intent,
                outcome = TimerOutcome.Disallow("Action already in flight")
            )
        }

        return try {
            val session = timingRepository.getActiveTimerSession(habitId)
            val timing = timingRepository.getHabitTiming(habitId)
            val remaining = remainingByHabit[habitId] ?: session?.remainingTime?.toMillis() ?: 0L
            val isPaused = pausedByHabit[habitId] ?: (session?.isPaused == true)

            val isRunning = remaining > 0 && !isPaused
            val askToCompleteWithoutTimer = timingPreferencesRepository.preferences().firstOrNull()?.askToCompleteWithoutTimer ?: true

            val redirectedIntent = when {
                intent == TimerIntent.Start && (isRunning || session?.isRunning == true) -> TimerIntent.Resume
                intent == TimerIntent.Start && session?.isPaused == true -> TimerIntent.Resume
                else -> intent
            }

            updateReservation(redirectedIntent, habitId)

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
                isInBreak = session?.isInBreak == true,
                requestedDurationSec = context.smartDuration?.seconds?.let { seconds ->
                    seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                },
                askToCompleteWithoutTimer = askToCompleteWithoutTimer
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
                ConfirmationOverride.LOG_PARTIAL_BELOW_MINIMUM -> {
                    if (redirectedIntent == TimerIntent.StopWithoutComplete || redirectedIntent == TimerIntent.Done) {
                        inputs.copy(logPartial = true)
                    } else {
                        inputs
                    }
                }

                ConfirmationOverride.COMPLETE_WITHOUT_TIMER -> {
                    if (redirectedIntent == TimerIntent.Done) {
                        inputs.copy(askToCompleteWithoutTimer = false)
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
                    promoteReservationToInFlight(redirectedIntent, habitId)
                    _state.value.copy(
                        trackedHabitId = habitId,
                        waitingForService = true,
                        lastOutcome = outcome,
                        timerState = timerState,
                        remainingMs = remaining,
                        paused = isPaused
                    )
                }
                else -> {
                    clearReservation(habitId)
                    _state.value.copy(lastOutcome = outcome)
                }
            }

            if (outcome is TimerOutcome.Execute) {
                dispatchActions(outcome.actions, habitId)
            }

            TimerActionDecision(resolvedIntent = redirectedIntent, outcome = outcome)
        } catch (throwable: Throwable) {
            clearReservation(habitId)
            throw throwable
        }
    }

    fun handleOutcome(decision: TimerActionDecision, habitId: Long) {
        val resolvedIntent = decision.resolvedIntent
        when (val outcome = decision.outcome) {
            is TimerOutcome.Execute -> {
                emitTelemetry(TimerActionTelemetry.Executed(habitId, resolvedIntent))
            }
            is TimerOutcome.Confirm -> {
                emitUiEvent(UiEvent.Confirm(habitId, outcome.type, outcome.payload))
                emitTelemetry(TimerActionTelemetry.Confirmed(habitId, resolvedIntent, outcome.type))
            }
            is TimerOutcome.Disallow -> {
                emitUiEvent(UiEvent.Snackbar(outcome.message))
                emitTelemetry(TimerActionTelemetry.Disallowed(habitId, resolvedIntent, outcome.message))
            }
        }
    }

    private fun shouldDebounce(intent: TimerIntent, habitId: Long, now: Long): Boolean {
        if (intent !in debouncedIntents) return false
        val stateSnapshot = _state.value
        if (stateSnapshot.waitingForService && stateSnapshot.trackedHabitId == habitId) return true
        if (reservedIntent != null && reservedHabitId == habitId) return true
        if (inFlightIntent != null && inFlightHabitId == habitId) return true
        val delta = now - lastActionAt
        return delta < debounceMs && stateSnapshot.trackedHabitId == habitId
    }

    private fun tryReserveIntent(intent: TimerIntent, habitId: Long, now: Long): Boolean {
        if (intent !in debouncedIntents) return true
        
        // Block actions if confirmation dialog is open for this habit
        if (_state.value.pendingConfirmHabitId == habitId) {
            return false
        }
        
        if (shouldDebounce(intent, habitId, now)) return false
        reservedIntent = intent
        reservedHabitId = habitId
        return true
    }

    private fun updateReservation(intent: TimerIntent, habitId: Long) {
        if (reservedHabitId == habitId) {
            reservedIntent = intent
        }
    }

    private fun promoteReservationToInFlight(intent: TimerIntent, habitId: Long) {
        if (reservedHabitId == habitId) {
            reservedIntent = null
            reservedHabitId = null
        }
        inFlightIntent = intent
        inFlightHabitId = habitId
    }

    private fun clearReservation(habitId: Long) {
        if (reservedHabitId == habitId) {
            reservedIntent = null
            reservedHabitId = null
        }
    }

    private fun dispatchActions(actions: List<TimerAction>, habitId: Long) {
        actions.forEach { action ->
            when (action) {
                is TimerAction.StartTimer -> {
                    val overrideDuration = action.durationOverrideSec?.let { Duration.ofSeconds(it.toLong()) }
                    timerController.start(action.habitId, duration = overrideDuration)
                }
                is TimerAction.PauseTimer -> timerController.pause()
                is TimerAction.ResumeTimer -> timerController.resume()
                is TimerAction.CompleteToday -> timerController.complete()
                is TimerAction.SavePartial -> {
                    timerController.stop()
                    appScope.launch {
                        runCatching {
                            timingRepository.logPartialSession(
                                habitId = action.habitId,
                                duration = Duration.ofSeconds(action.durationSec.toLong()),
                                note = null
                            )
                        }
                    }
                }
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
                emitUiEvent(UiEvent.Completed(event.habitId))
            }
            is TimerEvent.Error -> {
                if (_state.value.trackedHabitId == event.habitId) {
                    _state.value = _state.value.copy(
                        waitingForService = false,
                        trackedHabitId = null,
                        timerState = TimerState.IDLE,
                        remainingMs = 0L,
                        paused = false
                    )
                    inFlightIntent = null
                    inFlightHabitId = null
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
                inFlightHabitId = null
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
            inFlightHabitId = null
        }
    }

    private fun emitUiEvent(event: UiEvent) {
        _events.tryEmit(event)
    }

    private fun emitTelemetry(event: TimerActionTelemetry) {
        _telemetry.tryEmit(event)
    }

    /**
     * Signal that a confirmation dialog is being shown for a habit.
     * This blocks further actions on that habit until cleared.
     */
    fun setPendingConfirmation(habitId: Long, type: ConfirmType) {
        _state.value = _state.value.copy(
            pendingConfirmHabitId = habitId,
            pendingConfirmType = type
        )
    }

    /**
     * Clear the pending confirmation state, allowing actions to proceed again.
     */
    fun clearPendingConfirmation() {
        _state.value = _state.value.copy(
            pendingConfirmHabitId = null,
            pendingConfirmType = null
        )
    }

    companion object {
        private val debouncedIntents = setOf(
            TimerIntent.Start,
            TimerIntent.Resume,
            TimerIntent.Pause,
            TimerIntent.Done,
            TimerIntent.QuickComplete  // Prevent rapid checkmark clicks
        )
    }
}

data class TimerActionDecision(
    val resolvedIntent: TimerIntent,
    val outcome: TimerOutcome
)




