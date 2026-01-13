package com.habittracker.timerux

import com.habittracker.data.repository.HabitRepository
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
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerActionCoordinator @Inject constructor(
    private val interactor: TimerCompletionInteractor,
    private val habitRepository: HabitRepository,
    private val timingRepository: TimingRepository,
    private val timingPreferencesRepository: TimingPreferencesRepository,
    private val timerController: TimerController,
    @ApplicationScope private val appScope: CoroutineScope
) {

    data class CoordinatorState(
        // Active timer
        val trackedHabitId: Long? = null,
        val timerState: TimerState = TimerState.IDLE,
        val remainingMs: Long = 0L,
        val targetMs: Long = 0L,  // Phase 1: Total duration for progress calculation
        val paused: Boolean = false,
        
        // Loading/error state
        val isLoading: Boolean = false,  // Phase 1: Renamed from waitingForService
        val lastError: String? = null,   // Phase 1: Error state display
        
        // Auto-paused timer (for switch sheet)
        val pausedHabitId: Long? = null,      // Phase 1: Habit that was auto-paused
        val pausedRemainingMs: Long = 0L,     // Phase 1: Remaining time for paused habit
        
        // Decision outcome
        val lastOutcome: TimerOutcome? = null,
        
        // Confirmation tracking
        val pendingConfirmHabitId: Long? = null,  // Habit with open confirmation dialog
        val pendingConfirmType: ConfirmType? = null,  // Type of confirmation being shown
        
        // Overtime tracking (Phase UIX-11)
        val overtimeMs: Long = 0L,
        
        // Multi-timer support: Track all paused habits
        val pausedHabits: Set<Long> = emptySet()
    ) {
        // Backwards-compatible alias for existing code
        @Deprecated("Use isLoading instead", ReplaceWith("isLoading"))
        val waitingForService: Boolean get() = isLoading
    }

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
        // Timer completion fallback: Prompt user to complete when timer finishes without auto-complete
        data class CompletionPrompt(val habitId: Long, val message: String) : UiEvent
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
    
    // Timeout recovery: cancel waitingForService after this duration
    private val waitingTimeoutMs = 5000L
    private var waitingTimeoutJob: kotlinx.coroutines.Job? = null

    init {
        appScope.launch {
            TimerBus.events.collect(::onTimerEvent)
        }
    }
    
    /**
     * Starts a timeout that will reset isLoading state if no response
     * is received from the timer service within the timeout window.
     */
    private fun startWaitingTimeout(habitId: Long) {
        waitingTimeoutJob?.cancel()
        waitingTimeoutJob = appScope.launch {
            kotlinx.coroutines.delay(waitingTimeoutMs)
            // If still waiting for this habit, reset state and show error
            if (_state.value.isLoading && _state.value.trackedHabitId == habitId) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastError = "Action timed out. Please try again.",
                    lastOutcome = null
                )
                inFlightIntent = null
                inFlightHabitId = null
                emitUiEvent(UiEvent.Snackbar("Action timed out. Please try again."))
            }
        }
    }
    
    /**
     * Cancels any pending timeout when we receive a response.
     */
    private fun cancelWaitingTimeout() {
        waitingTimeoutJob?.cancel()
        waitingTimeoutJob = null
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

            val trackedId = _state.value.trackedHabitId
            val isStrictlyRunning = trackedId != null && !_state.value.paused
            
            // 1. If currently running, ONLY allow the running habit. Block everything else (even paused ones).
            if (isStrictlyRunning && trackedId != habitId && (intent == TimerIntent.Start || intent == TimerIntent.Resume)) {
                return TimerActionDecision(
                    resolvedIntent = intent,
                    outcome = TimerOutcome.Disallow("Finish your running habit first!")
                )
            }

            // 2. If not strictly running (e.g. paused), allow switching between PAUSED habits, 
            // but block NEW habits (Start) to enforce "Finish what you started".
            val isMyHabit = (trackedId == habitId) || (_state.value.pausedHabits.contains(habitId))
            val isBusy = trackedId != null || _state.value.pausedHabits.isNotEmpty()
            
            if (!isStrictlyRunning && isBusy && !isMyHabit && (intent == TimerIntent.Start || intent == TimerIntent.Resume)) {
                 return TimerActionDecision(
                    resolvedIntent = intent,
                    outcome = TimerOutcome.Disallow("Finish your paused habits first!")
                )
            }

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
                    // Start timeout to recover if service doesn't respond
                    startWaitingTimeout(habitId)
                    _state.value.copy(
                        trackedHabitId = habitId,
                        isLoading = true,
                        lastError = null,  // Clear any previous error
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
        if (stateSnapshot.isLoading && stateSnapshot.trackedHabitId == habitId) return true
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

    private suspend fun dispatchActions(actions: List<TimerAction>, habitId: Long) {
        actions.forEach { action ->
            when (action) {
                is TimerAction.StartTimer -> {
                    val overrideDuration = action.durationOverrideSec?.let { Duration.ofSeconds(it.toLong()) }
                    timerController.start(action.habitId, duration = overrideDuration)
                }
                is TimerAction.PauseTimer -> timerController.pause()
                is TimerAction.ResumeTimer -> {
                    // Fix: Ensure we resume the specific habit's session, enabling context switching
                    // If we just call resume(), it resumes the currently active service session (which might be the wrong habit)
                    val session = timingRepository.getActiveTimerSession(action.habitId)
                    if (session != null) {
                        timerController.resumeSession(action.habitId, session.id)
                    } else {
                        timerController.resume()
                    }
                }
                is TimerAction.CompleteToday -> {
                    if (action.persistDirectly) {
                        // No timer event expected; persist immediately and surface UI event
                        cancelWaitingTimeout()
                        _state.value = _state.value.copy(isLoading = false)
                        inFlightIntent = null
                        inFlightHabitId = null
                        
                        appScope.launch {
                            runCatching {
                                habitRepository.markHabitAsDone(action.habitId, LocalDate.now())
                            }.onFailure { emitUiEvent(UiEvent.Snackbar("Failed to mark complete: ${'$'}{it.message}")) }
                            emitUiEvent(UiEvent.Completed(action.habitId))
                        }
                    } else {
                        // Timer path: ask service to complete; persistence will occur on TimerEvent.Completed
                        timerController.complete()
                    }
                }
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
                    target = event.targetMs,  // Phase 1: Set targetMs
                    paused = false
                )
                _state.value = _state.value.copy(overtimeMs = 0L)
                updatePausedHabitsList()
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
                updatePausedHabitsList()
            }
            is TimerEvent.Resumed -> {
                pausedByHabit[event.habitId] = false
                updateTrackedState(
                    habitId = event.habitId,
                    waiting = false,
                    timerState = TimerState.RUNNING,
                    paused = false
                )
                updatePausedHabitsList()
            }
            is TimerEvent.Completed -> {
                remainingByHabit.remove(event.habitId)
                pausedByHabit.remove(event.habitId)
                appScope.launch {
                    runCatching {
                        habitRepository.markHabitAsDone(event.habitId, LocalDate.now())
                    }.onFailure { emitUiEvent(UiEvent.Snackbar("Failed to mark complete: ${'$'}{it.message}")) }
                }
                markCompleted(event.habitId)
                _state.value = _state.value.copy(overtimeMs = 0L)
                emitUiEvent(UiEvent.Completed(event.habitId))
                
                // Reminder: inactive paused habits
                val others = pausedByHabit.filter { it.value && it.key != event.habitId }
                if (others.isNotEmpty()) {
                    emitUiEvent(UiEvent.Snackbar("Nice job! You have ${others.size} other paused habit(s)."))
                }
                updatePausedHabitsList()
            }
            is TimerEvent.Error -> {
                if (_state.value.trackedHabitId == event.habitId) {
                    // Cancel timeout since we got a response (even if it's an error)
                    cancelWaitingTimeout()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        lastError = event.message,  // Phase 1: Store error in state
                        trackedHabitId = null,
                        timerState = TimerState.IDLE,
                        remainingMs = 0L,
                        targetMs = 0L,
                        paused = false,
                        overtimeMs = 0L
                    )
                    inFlightIntent = null
                    inFlightHabitId = null
                }
                emitUiEvent(UiEvent.Snackbar(event.message))
            }
            is TimerEvent.Extended -> {
                remainingByHabit[event.habitId] = event.newTargetMs
                if (_state.value.trackedHabitId == event.habitId) {
                    _state.value = _state.value.copy(
                        remainingMs = event.newTargetMs,
                        targetMs = event.newTargetMs  // Phase 1: Update targetMs on extend
                    )
                }
            }
            is TimerEvent.ReachedTarget -> {
                // Timer completion fallback: Timer finished without auto-complete
                remainingByHabit[event.habitId] = 0L
                updateTrackedState(
                    habitId = event.habitId,
                    waiting = false,
                    timerState = TimerState.AT_TARGET,
                    remaining = 0L,
                    paused = false
                )
                // Emit completion prompt UI event
                emitUiEvent(UiEvent.CompletionPrompt(
                    habitId = event.habitId,
                    message = "Timer finished! Tap to complete."
                ))
            }
            is TimerEvent.Overtime -> {
                if (_state.value.trackedHabitId == event.habitId) {
                    _state.value = _state.value.copy(overtimeMs = event.overtimeMs)
                }
            }
            is TimerEvent.AutoPaused -> {
                // Phase 1: Track the auto-paused habit for switch sheet display
                pausedByHabit[event.pausedHabitId] = true
                val pausedRemaining = remainingByHabit[event.pausedHabitId] ?: 0L
                _state.value = _state.value.copy(
                    pausedHabitId = event.pausedHabitId,
                    pausedRemainingMs = pausedRemaining
                )
                updatePausedHabitsList()
            }
            else -> Unit
        }
    }

    private fun updateTrackedState(
        habitId: Long,
        waiting: Boolean,
        timerState: TimerState,
        remaining: Long? = null,
        target: Long? = null,
        paused: Boolean
    ) {
        if (_state.value.trackedHabitId == habitId) {
            _state.value = _state.value.copy(
                isLoading = waiting,
                timerState = timerState,
                remainingMs = remaining ?: _state.value.remainingMs,
                targetMs = target ?: _state.value.targetMs,
                paused = paused
            )
            if (!waiting) {
                // Cancel timeout since we got a response
                cancelWaitingTimeout()
                inFlightIntent = null
                inFlightHabitId = null
            }
        }
    }

    private fun markCompleted(habitId: Long) {
        if (_state.value.trackedHabitId == habitId) {
            // Cancel timeout since we got a response
            cancelWaitingTimeout()
            _state.value = _state.value.copy(
                isLoading = false,
                trackedHabitId = null,
                timerState = TimerState.IDLE,
                remainingMs = 0L,
                targetMs = 0L,
                paused = false,
                overtimeMs = 0L
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

    /**
     * Phase 1: Clear the auto-paused habit state.
     * Call this when user dismisses the switch sheet or resumes the paused timer.
     */
    fun clearPausedHabit() {
        _state.value = _state.value.copy(
            pausedHabitId = null,
            pausedRemainingMs = 0L
        )
    }

    /**
     * Phase 1: Clear any error state.
     * Call this when user dismisses the error banner or after auto-timeout.
     */
    fun clearError() {
        _state.value = _state.value.copy(lastError = null)
    }

    private fun updatePausedHabitsList() {
        val paused = pausedByHabit.filter { it.value }.keys.toSet()
        _state.value = _state.value.copy(pausedHabits = paused)
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




