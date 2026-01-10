package com.habittracker.ui.viewmodels.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.timing.TimerBus
import com.habittracker.timing.TimerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Minimal ViewModel that tracks remaining time per habit from TimerBus events.
 * Used by UI to show a live countdown without heavy wiring.
 * 
 * @deprecated This ViewModel is deprecated as part of the timer state simplification (Phase 3).
 * Use [com.habittracker.timerux.TimerActionCoordinator.state] instead, which provides
 * a single source of truth for all timer state. This ViewModel duplicates coordinator
 * state and will be removed in the next major version.
 * 
 * Migration guide:
 * - Replace `timerTickerViewModel.remainingByHabit` with `coordinator.state.remainingMs`
 * - Replace `timerTickerViewModel.pausedByHabit` with `coordinator.state.paused`
 * - Replace `timerTickerViewModel.errorsByHabit` with `coordinator.state.lastError`
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
class TimerTickerViewModel @Inject constructor() : ViewModel() {
    private val _remainingByHabit = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val remainingByHabit: StateFlow<Map<Long, Long>> = _remainingByHabit.asStateFlow()
    private val _pausedByHabit = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val pausedByHabit: StateFlow<Map<Long, Boolean>> = _pausedByHabit.asStateFlow()
    private val _errorsByHabit = MutableStateFlow<Map<Long, String>>(emptyMap())
    val errorsByHabit: StateFlow<Map<Long, String>> = _errorsByHabit.asStateFlow()
    private val _overtimeByHabit = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val overtimeByHabit: StateFlow<Map<Long, Long>> = _overtimeByHabit.asStateFlow()
    // Cache last emitted whole-second value per habit to avoid noisy UI updates
    private val lastSecondByHabit = mutableMapOf<Long, Long>()

    init {
        viewModelScope.launch {
            TimerBus.events.collect { event ->
                when (event) {
                    is TimerEvent.Started -> {
                        _remainingByHabit.value = _remainingByHabit.value + (event.habitId to event.targetMs)
                        _pausedByHabit.value = _pausedByHabit.value + (event.habitId to false)
                        _errorsByHabit.value = _errorsByHabit.value - event.habitId
                        lastSecondByHabit[event.habitId] = (event.targetMs / 1000L)
                    }
                    is TimerEvent.Tick -> {
                        val sec = event.remainingMs.coerceAtLeast(0L) / 1000L
                        val last = lastSecondByHabit[event.habitId]
                        if (last == null || last != sec) {
                            lastSecondByHabit[event.habitId] = sec
                            _remainingByHabit.value = _remainingByHabit.value + (event.habitId to event.remainingMs)
                        }
                    }
                    is TimerEvent.Completed -> {
                        _remainingByHabit.value = _remainingByHabit.value - event.habitId
                        _pausedByHabit.value = _pausedByHabit.value - event.habitId
                        _errorsByHabit.value = _errorsByHabit.value - event.habitId
                        _overtimeByHabit.value = _overtimeByHabit.value - event.habitId
                        lastSecondByHabit.remove(event.habitId)
                    }
                    is TimerEvent.Paused -> {
                        _pausedByHabit.value = _pausedByHabit.value + (event.habitId to true)
                    }
                    is TimerEvent.Resumed -> {
                        _pausedByHabit.value = _pausedByHabit.value + (event.habitId to false)
                    }
                    is TimerEvent.Extended -> {
                        // Set remaining to previous remaining + addedMs (will adjust on next tick naturally)
                        val current = _remainingByHabit.value[event.habitId] ?: event.newTargetMs
                        val approxRemaining = (current + event.addedMs).coerceAtMost(event.newTargetMs)
                        _remainingByHabit.value = _remainingByHabit.value + (event.habitId to approxRemaining)
                        lastSecondByHabit[event.habitId] = (approxRemaining.coerceAtLeast(0L) / 1000L)
                    }
                    is TimerEvent.Error -> {
                        _errorsByHabit.value = _errorsByHabit.value + (event.habitId to event.message)
                    }
                    is TimerEvent.Overtime -> {
                        _overtimeByHabit.value = _overtimeByHabit.value + (event.habitId to event.overtimeMs)
                    }
                    is TimerEvent.NearTarget -> {
                        // Optional: could surface a UI hint if desired; no-op here.
                    }
                    else -> { /* ignore future events */ }
                }
            }
        }
    }
}
