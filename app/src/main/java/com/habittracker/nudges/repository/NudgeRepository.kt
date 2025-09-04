package com.habittracker.nudges.repository

import com.habittracker.nudges.model.Nudge
import com.habittracker.nudges.model.NudgeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing nudges and their persistence
 */
@Singleton
class NudgeRepository @Inject constructor() {
    
    private val _activeNudges = MutableStateFlow<List<Nudge>>(emptyList())
    val activeNudges: Flow<List<Nudge>> = _activeNudges.asStateFlow()
    
    private val _config = MutableStateFlow(NudgeConfig())
    val config: Flow<NudgeConfig> = _config.asStateFlow()
    
    private val nudgeHistory = mutableListOf<Nudge>()
    
    /**
     * Adds new nudges to the active list
     */
    suspend fun addNudges(nudges: List<Nudge>) {
        nudgeHistory.addAll(nudges)
        updateActiveNudges()
    }
    
    /**
     * Dismisses a nudge by ID
     */
    suspend fun dismissNudge(nudgeId: String) {
        val nudgeIndex = nudgeHistory.indexOfFirst { it.id == nudgeId }
        if (nudgeIndex != -1) {
            nudgeHistory[nudgeIndex] = nudgeHistory[nudgeIndex].copy(isDismissed = true)
            updateActiveNudges()
        }
    }
    
    /**
     * Marks a nudge action as taken
     */
    suspend fun markActionTaken(nudgeId: String) {
        val nudgeIndex = nudgeHistory.indexOfFirst { it.id == nudgeId }
        if (nudgeIndex != -1) {
            nudgeHistory[nudgeIndex] = nudgeHistory[nudgeIndex].copy(isActionTaken = true)
            updateActiveNudges()
        }
    }
    
    /**
     * Updates the nudge configuration
     */
    suspend fun updateConfig(newConfig: NudgeConfig) {
        _config.value = newConfig
    }
    
    /**
     * Gets the current configuration
     */
    fun getCurrentConfig(): NudgeConfig = _config.value
    
    /**
     * Updates the active nudges list by filtering out dismissed and expired nudges
     */
    private fun updateActiveNudges() {
        val now = LocalDateTime.now()
        val active = nudgeHistory.filter { nudge ->
            !nudge.isDismissed && 
            (nudge.expiresAt == null || nudge.expiresAt.isAfter(now))
        }.sortedByDescending { it.priority.ordinal }
        
        _activeNudges.value = active
    }
    
    /**
     * Gets all nudges for a specific habit
     */
    fun getNudgesForHabit(habitId: Long): List<Nudge> {
        return nudgeHistory.filter { it.habitId == habitId }
    }
    
    /**
     * Gets nudges created today
     */
    fun getTodayNudges(): List<Nudge> {
        val today = LocalDateTime.now().toLocalDate()
        return nudgeHistory.filter { 
            it.createdAt.toLocalDate() == today
        }
    }
    
    /**
     * Cleans up old nudges (older than 30 days)
     */
    suspend fun cleanupOldNudges() {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        nudgeHistory.removeAll { it.createdAt.isBefore(thirtyDaysAgo) }
        updateActiveNudges()
    }
    
    /**
     * Gets nudge statistics
     */
    fun getNudgeStats(): NudgeStats {
        val total = nudgeHistory.size
        val dismissed = nudgeHistory.count { it.isDismissed }
        val actionTaken = nudgeHistory.count { it.isActionTaken }
        val active = _activeNudges.value.size
        
        return NudgeStats(
            totalNudges = total,
            dismissedNudges = dismissed,
            actionTakenNudges = actionTaken,
            activeNudges = active,
            engagementRate = if (total > 0) actionTaken.toDouble() / total else 0.0
        )
    }
}

/**
 * Statistics about nudge usage
 */
data class NudgeStats(
    val totalNudges: Int,
    val dismissedNudges: Int,
    val actionTakenNudges: Int,
    val activeNudges: Int,
    val engagementRate: Double
)
