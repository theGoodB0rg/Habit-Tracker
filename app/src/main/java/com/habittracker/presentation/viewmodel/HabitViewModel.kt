package com.habittracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.analytics.domain.models.DifficultyLevel
import com.habittracker.analytics.domain.usecases.TrackingUseCases
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.database.entity.HabitFrequency
import com.habittracker.data.repository.HabitRepository
import com.habittracker.domain.model.HabitStats
import com.habittracker.domain.model.HabitStreak
import com.habittracker.ui.models.HabitUiModel
import com.habittracker.ui.models.toUiModelWithTiming
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Enhanced ViewModel for Phase 2 - Advanced habit management
 * Handles complex habit operations, streaks, and statistics with analytics tracking
 */
@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val trackingUseCases: TrackingUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
    
    private val _habitStreaks = MutableStateFlow<Map<Long, HabitStreak>>(emptyMap())
    val habitStreaks: StateFlow<Map<Long, HabitStreak>> = _habitStreaks.asStateFlow()
    
    private val _todayCompletions = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val todayCompletions: StateFlow<Map<Long, Boolean>> = _todayCompletions.asStateFlow()
    
    private val _habitsState = MutableStateFlow<List<HabitEntity>>(emptyList())
    val habits: StateFlow<List<HabitEntity>> = _habitsState.asStateFlow()

    // Phase 2: Hydrated Habit UI models (with timing data)
    private val _hydratedHabits = MutableStateFlow<List<HabitUiModel>>(emptyList())
    val hydratedHabits: StateFlow<List<HabitUiModel>> = _hydratedHabits.asStateFlow()
    // Track if initial data load has occurred to distinguish "Loading" from "Empty/Deleted"
    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    // Trigger to force refresh of hydrated habits (e.g., when timing data changes)
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    
    init {
        loadInitialData()
        loadTodayCompletions()
    hydrateHabitsWithTiming()
    }

    // Phase UIX-8: expose recent completed timer sessions for analytics card
    suspend fun getRecentCompletedTimerSessions(habitId: Long, limit: Int): List<com.habittracker.ui.models.timing.TimerSession> {
        return habitRepository.getRecentCompletedTimerSessions(habitId, limit)
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Check if we need to insert dummy data
                val habitsCount = habitRepository.getActiveHabitsCount()
                if (habitsCount == 0) {
                    habitRepository.insertEnhancedDummyData()
                }
                
                // Load streak information for all habits
                refreshStreaks()
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load habits: ${e.message}"
                )
            }
        }
    }



    // Phase 2: Build HabitUiModel list with timing, active session, and suggestions
    private fun hydrateHabitsWithTiming() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                habitRepository.getAllHabits(),
                _refreshTrigger
            ) { entities, _ -> entities }
            .collectLatest { entities ->
                _habitsState.value = entities
                val models = entities.map { entity ->
                    val timing = habitRepository.getHabitTiming(entity.id)
                    val session = habitRepository.getActiveTimerSession(entity.id)
                    val suggestions = habitRepository.getSmartSuggestions(entity.id)
                    val metrics = habitRepository.getCompletionMetrics(entity.id).first()
                    entity.toUiModelWithTiming(
                        timing = timing,
                        timerSession = session,
                        smartSuggestions = suggestions,
                        completionMetrics = metrics
                    )
                }
                _hydratedHabits.value = models
                _isDataLoaded.value = true
            }
        }
    }
    
    /**
     * Add a new habit with the provided details
     */
    fun addHabit(
        name: String,
        description: String,
        frequency: String,
        iconId: Int,
        timerEnabled: Boolean = true,
        customDurationMinutes: Int? = null,
        minDurationMinutes: Int? = null,
        requireTimerToComplete: Boolean = false,
        autoCompleteOnTarget: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val habit = HabitEntity(
                    name = name,
                    description = description,
                    frequency = HabitFrequency.valueOf(frequency.uppercase()),
                    iconId = iconId,
                    createdDate = java.util.Date(),
                    streakCount = 0,
                    lastCompletedDate = null,
                    timerEnabled = timerEnabled,
                    customDurationMinutes = customDurationMinutes
                )
                
                val habitId = habitRepository.insertHabit(habit)
                
                // Create timing configuration if timer settings provided
                if (timerEnabled && (minDurationMinutes != null || requireTimerToComplete || autoCompleteOnTarget || customDurationMinutes != null)) {
                    val duration = customDurationMinutes?.let { java.time.Duration.ofMinutes(it.toLong()) }
                    val minDuration = minDurationMinutes?.let { java.time.Duration.ofMinutes(it.toLong()) }
                    
                    val timing = com.habittracker.ui.models.timing.HabitTiming(
                        estimatedDuration = duration,
                        minDuration = minDuration,
                        requireTimerToComplete = requireTimerToComplete,
                        autoCompleteOnTarget = autoCompleteOnTarget,
                        timerEnabled = timerEnabled
                    )
                    habitRepository.saveHabitTiming(habitId, timing)
                    _refreshTrigger.tryEmit(Unit)
                }
                
                refreshStreaks()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Habit '$name' added successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to add habit: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update an existing habit with new details
     */
    fun updateHabit(
        habitId: Long,
        name: String,
        description: String,
        frequency: String,
    iconId: Int,
    timerEnabledOverride: Boolean? = null,
    customDurationMinutesOverride: Int? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get the existing habit first
                val existingHabit = habitRepository.getHabitById(habitId)
                if (existingHabit != null) {
                    val updatedHabit = existingHabit.copy(
                        name = name,
                        description = description,
                        frequency = HabitFrequency.valueOf(frequency.uppercase()),
                        iconId = iconId,
                        timerEnabled = timerEnabledOverride ?: existingHabit.timerEnabled,
                        customDurationMinutes = if (timerEnabledOverride == false) null else customDurationMinutesOverride?.takeIf { it > 0 },
                        // Keep existing alertProfileId until profile editor available
                        alertProfileId = existingHabit.alertProfileId
                    )
                    
                    habitRepository.updateHabit(updatedHabit)
                    refreshStreaks()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Habit '$name' updated successfully!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Habit not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update habit: ${e.message}"
                )
            }
        }
    }

    // Phase 2: Save per-habit timing preferences (e.g., auto-complete on target)
    fun saveHabitTiming(
        habitId: Long,
        timing: com.habittracker.ui.models.timing.HabitTiming
    ) {
        viewModelScope.launch {
            try {
                habitRepository.saveHabitTiming(habitId, timing)
                // Refresh hydrated models so UI reflects new timing config
                _refreshTrigger.tryEmit(Unit)
                _uiState.value = _uiState.value.copy(successMessage = "Timing updated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update timing: ${e.message}")
            }
        }
    }
    
    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                habitRepository.deleteHabit(habitId)
                refreshStreaks()
                loadTodayCompletions()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Habit deleted successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete habit: ${e.message}"
                )
            }
        }
    }

    // Phase 2: Getter for per-habit timing (used by Edit screen)
    suspend fun getHabitTiming(habitId: Long): com.habittracker.ui.models.timing.HabitTiming? {
        return habitRepository.getHabitTiming(habitId)
    }
    
    /**
     * Mark habit as complete for today with enhanced streak tracking and analytics
     */
    fun markHabitComplete(habitId: Long, note: String? = null) {
        viewModelScope.launch {
            try {
                val habit = habitRepository.getHabitById(habitId)
                val streak = habitRepository.markHabitAsDone(habitId, LocalDate.now(), note)
                
                // Track completion in analytics
                if (habit != null) {
                    val difficultyLevel = mapHabitDifficulty(habit)
                    trackingUseCases.trackHabitCompletionUseCase(
                        habitId = habitId.toString(),
                        habitName = habit.name,
                        isCompleted = true,
                        timeSpentMinutes = estimateTimeSpent(habit),
                        difficultyLevel = difficultyLevel
                    )

                    // Slice 3: Record completion metrics and update analytics
                    val completionTime = java.time.LocalTime.now()
                    val estMinutes = estimateTimeSpent(habit).toLong()
                    habitRepository.recordCompletionMetrics(
                        habitId = habitId,
                        completionTime = completionTime,
                        duration = java.time.Duration.ofMinutes(estMinutes),
                        efficiency = null
                    )
                    habitRepository.updateHabitAnalytics(habitId)
                }
                
                // Update local state
                val currentStreaks = _habitStreaks.value.toMutableMap()
                currentStreaks[habitId] = streak
                _habitStreaks.value = currentStreaks
                
                // Update today's completions
                val currentCompletions = _todayCompletions.value.toMutableMap()
                currentCompletions[habitId] = true
                _todayCompletions.value = currentCompletions
                
                _uiState.value = _uiState.value.copy(
                    successMessage = "Habit completed! Current streak: ${streak.currentStreak} 🔥"
                )

                // Refresh hydrated models so analytics appear immediately on detail screen
                try {
                    val entities = habitRepository.getAllHabits().first()
                    val models = entities.map { entity ->
                        val timing = habitRepository.getHabitTiming(entity.id)
                        val session = habitRepository.getActiveTimerSession(entity.id)
                        val suggestions = habitRepository.getSmartSuggestions(entity.id)
                        val metrics = habitRepository.getCompletionMetrics(entity.id).first()
                        entity.toUiModelWithTiming(
                            timing = timing,
                            timerSession = session,
                            smartSuggestions = suggestions,
                            completionMetrics = metrics
                        )
                    }
                    _hydratedHabits.value = models
                } catch (_: Exception) {
                    // Non-fatal: UI will catch up on next hydrate cycle
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to mark habit complete: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Unmark habit completion for today with analytics tracking
     */
    fun unmarkHabitForToday(habitId: Long) {
        viewModelScope.launch {
            try {
                val habit = habitRepository.getHabitById(habitId)
                habitRepository.unmarkHabitForDate(habitId, LocalDate.now())
                
                // Track uncompleted habit in analytics
                if (habit != null) {
                    val difficultyLevel = mapHabitDifficulty(habit)
                    trackingUseCases.trackHabitCompletionUseCase(
                        habitId = habitId.toString(),
                        habitName = habit.name,
                        isCompleted = false,
                        timeSpentMinutes = 0,
                        difficultyLevel = difficultyLevel
                    )
                }
                
                // Refresh streak information
                val streak = habitRepository.getCurrentStreak(habitId)
                val currentStreaks = _habitStreaks.value.toMutableMap()
                currentStreaks[habitId] = streak
                _habitStreaks.value = currentStreaks
                
                // Update today's completions
                val currentCompletions = _todayCompletions.value.toMutableMap()
                currentCompletions[habitId] = false
                _todayCompletions.value = currentCompletions
                
                _uiState.value = _uiState.value.copy(
                    successMessage = "Habit unmarked for today"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to unmark habit: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get detailed statistics for a habit
     */
    fun getHabitStats(habitId: Long) {
        viewModelScope.launch {
            try {
                val stats = habitRepository.getHabitStats(habitId)
                _uiState.value = _uiState.value.copy(
                    selectedHabitStats = stats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load habit stats: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Check for habits at risk of losing streaks
     */
    fun checkHabitsAtRisk() {
        viewModelScope.launch {
            try {
                val riskyHabits = habitRepository.getHabitsAtRisk()
                if (riskyHabits.isNotEmpty()) {
                    val habitNames = riskyHabits.map { it.name }
                    _uiState.value = _uiState.value.copy(
                        warningMessage = "⚠️ Streak at risk: ${habitNames.joinToString(", ")}"
                    )
                }
            } catch (e: Exception) {
                // Silently handle this - it's not critical
            }
        }
    }
    
    private fun refreshStreaks() {
        viewModelScope.launch {
            try {
                // Get current habits from repository (first emission)
                habitRepository.getAllHabits().collect { currentHabits ->
                    if (currentHabits.isNotEmpty()) {
                        val streaksMap = mutableMapOf<Long, HabitStreak>()
                        
                        for (habit in currentHabits) {
                            val streak = habitRepository.getCurrentStreak(habit.id)
                            streaksMap[habit.id] = streak
                        }
                        
                        _habitStreaks.value = streaksMap
                        return@collect // Exit after first emission
                    }
                }
            } catch (e: Exception) {
                // Handle silently for now
            }
        }
    }
    
    private fun loadTodayCompletions() {
        viewModelScope.launch {
            try {
                val completions = habitRepository.getTodayCompletionStatus()
                _todayCompletions.value = completions
            } catch (e: Exception) {
                // Handle silently for now
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            warningMessage = null
        )
    }
    
    fun refreshData() {
        loadInitialData()
        loadTodayCompletions()
        checkHabitsAtRisk()
    }
    
    fun clearSelectedHabitStats() {
        _uiState.value = _uiState.value.copy(selectedHabitStats = null)
    }

    // UIX-6: Set per-habit alert profile
    fun setHabitAlertProfile(habitId: Long, profileId: String?) {
        viewModelScope.launch {
            try {
                habitRepository.setAlertProfile(habitId, profileId)
            } catch (_: Exception) {
                // Non-fatal; UI will stay unchanged if write fails
            }
        }
    }
    
    /**
     * Map habit characteristics to analytics difficulty level
     */
    private fun mapHabitDifficulty(habit: HabitEntity): DifficultyLevel {
        return when {
            habit.frequency == HabitFrequency.DAILY && habit.streakCount >= 30 -> DifficultyLevel.EXPERT
            habit.frequency == HabitFrequency.DAILY && habit.streakCount >= 7 -> DifficultyLevel.HARD
            habit.frequency == HabitFrequency.WEEKLY || habit.streakCount >= 3 -> DifficultyLevel.MODERATE
            else -> DifficultyLevel.EASY
        }
    }
    
    /**
     * Estimate time spent on habit based on its characteristics
     */
    private fun estimateTimeSpent(habit: HabitEntity): Int {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> when {
                habit.name.contains("workout", ignoreCase = true) || 
                habit.name.contains("exercise", ignoreCase = true) -> 45
                habit.name.contains("read", ignoreCase = true) -> 30
                habit.name.contains("meditat", ignoreCase = true) -> 15
                habit.name.contains("journal", ignoreCase = true) -> 10
                else -> 20
            }
            HabitFrequency.WEEKLY -> 60
            HabitFrequency.MONTHLY -> 120
        }
    }
}

/**
 * Enhanced UI state for Phase 2 with additional properties
 */
data class HabitUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val warningMessage: String? = null,
    val selectedHabitStats: HabitStats? = null
)
