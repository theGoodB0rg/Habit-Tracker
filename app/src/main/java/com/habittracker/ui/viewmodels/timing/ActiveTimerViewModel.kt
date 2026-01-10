package com.habittracker.ui.viewmodels.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.timing.TimerBus
import com.habittracker.timing.TimerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI-facing state for an active timer session.
 * 
 * @deprecated This ViewModel is deprecated as part of the timer state simplification (Phase 3).
 * Use [com.habittracker.timerux.TimerActionCoordinator.state] instead, which provides
 * a single source of truth for all timer state. This ViewModel duplicates coordinator
 * state and will be removed in the next major version.
 * 
 * Migration guide:
 * - Replace `activeTimerViewModel.state.active` with `coordinator.state.trackedHabitId != null`
 * - Replace `activeTimerViewModel.state.remainingMs` with `coordinator.state.remainingMs`
 * - Replace `activeTimerViewModel.state.totalMs` with `coordinator.state.targetMs`
 * - Replace `activeTimerViewModel.state.paused` with `coordinator.state.paused`
 * - Replace `activeTimerViewModel.state.habitId` with `coordinator.state.trackedHabitId`
 * 
 * TODO: Remove in next major version
 */
@Deprecated(
    message = "Use TimerActionCoordinator.state instead. This ViewModel duplicates coordinator state.",
    replaceWith = ReplaceWith(
        "timerActionHandler.state",
        "com.habittracker.timerux.TimerActionCoordinator"
    ),
    level = DeprecationLevel.WARNING
)
@HiltViewModel
class ActiveTimerViewModel @Inject constructor(): ViewModel() {
    data class TimerUiState(
        val active: Boolean = false,
        val totalMs: Long = 0L,
        val remainingMs: Long = 0L,
        val habitId: Long = 0L,
    val sessionId: Long = 0L,
    val paused: Boolean = false,
    val autoComplete: Boolean = false
    )
    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            TimerBus.events.collect { evt ->
                when (evt) {
                    is TimerEvent.Started -> _state.value = _state.value.copy(
                        active = true,
                        sessionId = evt.sessionId,
                        habitId = evt.habitId,
                        totalMs = evt.targetMs,
                        remainingMs = evt.targetMs,
                        paused = false,
                        autoComplete = evt.autoComplete
                    )
                    is TimerEvent.Tick -> if (_state.value.active && evt.sessionId == _state.value.sessionId) {
                        _state.value = _state.value.copy(remainingMs = evt.remainingMs)
                    }
                    is TimerEvent.Paused -> if (evt.sessionId == _state.value.sessionId) {
                        _state.value = _state.value.copy(paused = true)
                    }
                    is TimerEvent.Resumed -> if (evt.sessionId == _state.value.sessionId) {
                        _state.value = _state.value.copy(paused = false)
                    }
                    is TimerEvent.Completed -> if (evt.sessionId == _state.value.sessionId) {
                        _state.value = _state.value.copy(active = false, remainingMs = 0L)
                    }
                    is TimerEvent.Extended -> if (evt.sessionId == _state.value.sessionId) {
                        // Increase total target and keep remaining as previously computed (will update on next tick)
                        _state.value = _state.value.copy(totalMs = evt.newTargetMs)
                    }
                    is TimerEvent.Error -> { /* ignore */ }
                    is TimerEvent.Overtime -> { /* surfaced via TimerTickerViewModel */ }
                    is TimerEvent.NearTarget -> { /* no-op here; sheet/notification handles nudge */ }
                    else -> { /* ignore future events */ }
                }
            }
        }
    }
}
