package com.habittracker.nudges.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.nudges.model.Nudge
import com.habittracker.nudges.repository.NudgeRepository
import com.habittracker.nudges.repository.NudgeStats
import com.habittracker.nudges.usecase.GenerateNudgesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing nudge-related UI state and operations
 */
@HiltViewModel
class NudgeViewModel @Inject constructor(
    private val generateNudgesUseCase: GenerateNudgesUseCase,
    private val nudgeRepository: NudgeRepository
) : ViewModel() {
    
    val activeNudges: StateFlow<List<Nudge>> = generateNudgesUseCase.getActiveNudges()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val nudgeConfig = nudgeRepository.config
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = nudgeRepository.getCurrentConfig()
        )
    
    private val _nudgeStats = MutableStateFlow(NudgeStats(0, 0, 0, 0, 0.0))
    val nudgeStats: StateFlow<NudgeStats> = _nudgeStats.asStateFlow()
    
    init {
        // Update stats whenever active nudges change
        viewModelScope.launch {
            activeNudges.collect {
                updateNudgeStats()
            }
        }
        
        // Periodic cleanup of old nudges
        viewModelScope.launch {
            nudgeRepository.cleanupOldNudges()
        }
    }
    
    /**
     * Dismisses a nudge by its ID
     */
    fun dismissNudge(nudgeId: String) {
        viewModelScope.launch {
            generateNudgesUseCase.dismissNudge(nudgeId)
        }
    }
    
    /**
     * Marks a nudge action as taken and dismisses the nudge
     */
    fun takeNudgeAction(nudgeId: String) {
        viewModelScope.launch {
            generateNudgesUseCase.takeNudgeAction(nudgeId)
        }
    }
    
    /**
     * Gets the count of active nudges
     */
    fun getActiveNudgeCount(): Int {
        return activeNudges.value.size
    }
    
    /**
     * Gets nudges filtered by priority
     */
    fun getNudgesByPriority() = activeNudges.map { nudges ->
        nudges.groupBy { it.priority }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    
    /**
     * Updates nudge statistics
     */
    private fun updateNudgeStats() {
        viewModelScope.launch {
            _nudgeStats.value = nudgeRepository.getNudgeStats()
        }
    }
    
    /**
     * Dismisses all active nudges
     */
    fun dismissAllNudges() {
        viewModelScope.launch {
            activeNudges.value.forEach { nudge ->
                generateNudgesUseCase.dismissNudge(nudge.id)
            }
        }
    }
}
