package com.habittracker.ui.viewmodels.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.data.repository.timing.TimingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HabitTimingAnalyticsViewModel @Inject constructor(
    private val timingRepository: TimingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitTimingAnalyticsUiState())
    val uiState: StateFlow<HabitTimingAnalyticsUiState> = _uiState.asStateFlow()

    fun load(habitId: Long) {
        // Idempotent load: avoid reloading same habit repeatedly
        if (_uiState.value.habitId == habitId && !_uiState.value.needsReload) return

        _uiState.value = HabitTimingAnalyticsUiState(habitId = habitId, loading = true)
        viewModelScope.launch {
            try {
                // Small debounce to coalesce bursts of calls when screen recomposes
                delay(120)
                // Fetch last N completed sessions for sparkline
                val sessions = timingRepository.getRecentCompletedTimerSessions(habitId, limit = 14)
                val durations = sessions.mapNotNull { it.actualDuration.toMinutes().toInt() }
                    .filter { it > 0 } // Filter out invalid durations
                    .takeIf { it.isNotEmpty() } ?: emptyList()

                // Determine target from latest session or habit timing
                val targetFromSession = sessions.firstOrNull()?.targetDuration?.toMinutes()?.toInt()
                val target = targetFromSession
                    ?: timingRepository.getHabitTiming(habitId)?.estimatedDuration?.toMinutes()?.toInt()

                val average = if (durations.isNotEmpty()) (durations.average()).toInt() else null
                val adherence = if (average != null && target != null && target > 0) {
                    (average * 100f / target).toInt().coerceIn(0, 200)
                } else null

                // Pull a current suggestion for explanation (best-effort)
                val suggestion = runCatching { timingRepository.getSmartSuggestions(habitId).firstOrNull() }.getOrNull()

                // Live metrics (optional)
                val liveMetrics = runCatching { timingRepository.getCompletionMetrics(habitId).first() }.getOrNull()

                val newState = _uiState.value.copy(
                    loading = false,
                    dataPoints = durations,
                    targetMinutes = target,
                    averageDuration = average,
                    adherencePercent = adherence,
                    suggestionTitle = suggestion?.type?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.titlecase() },
                    suggestionReason = suggestion?.reason,
                    totalCompletions = liveMetrics?.totalCompletions ?: durations.size
                )
                if (newState != _uiState.value) {
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load analytics",
                    needsReload = true
                )
            }
        }
    }
}

data class HabitTimingAnalyticsUiState(
    val habitId: Long? = null,
    val loading: Boolean = false,
    val dataPoints: List<Int> = emptyList(), // minutes per recent session
    val targetMinutes: Int? = null,
    val averageDuration: Int? = null,
    val adherencePercent: Int? = null,
    val totalCompletions: Int = 0,
    val suggestionTitle: String? = null,
    val suggestionReason: String? = null,
    val error: String? = null,
    val needsReload: Boolean = false
)
