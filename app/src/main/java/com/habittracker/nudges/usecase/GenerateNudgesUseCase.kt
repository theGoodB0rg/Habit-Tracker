package com.habittracker.nudges.usecase

import com.habittracker.domain.model.HabitStats
import com.habittracker.nudges.analyzer.HabitPatternAnalyzer
import com.habittracker.nudges.engine.NudgeEngine
import com.habittracker.nudges.model.Nudge
import com.habittracker.nudges.model.NudgeContext
import com.habittracker.nudges.repository.NudgeRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for generating and managing behavioral nudges
 */
@Singleton
class GenerateNudgesUseCase @Inject constructor(
    private val nudgeEngine: NudgeEngine,
    private val patternAnalyzer: HabitPatternAnalyzer,
    private val nudgeRepository: NudgeRepository
) {
    
    /**
     * Generates nudges for a list of habits with their statistics
     */
    suspend fun generateNudges(habitsData: List<HabitAnalysisData>): List<Nudge> {
        // Analyze each habit to create context
        val contexts = habitsData.map { habitData ->
            patternAnalyzer.analyzeHabitPattern(
                habitId = habitData.habitId,
                habitName = habitData.habitName,
                stats = habitData.stats,
                lastCompletedDate = habitData.lastCompletedDate,
                isCompletedToday = habitData.isCompletedToday
            )
        }
        
        // Generate nudges based on contexts
        val generatedNudges = nudgeEngine.generateNudges(contexts)
        
        // Store nudges in repository
        if (generatedNudges.isNotEmpty()) {
            nudgeRepository.addNudges(generatedNudges)
        }
        
        return generatedNudges
    }
    
    /**
     * Generates nudges for a single habit
     */
    suspend fun generateNudgesForHabit(habitData: HabitAnalysisData): List<Nudge> {
        return generateNudges(listOf(habitData))
    }
    
    /**
     * Gets the current active nudges
     */
    fun getActiveNudges(): Flow<List<Nudge>> {
        return nudgeRepository.activeNudges
    }
    
    /**
     * Dismisses a nudge
     */
    suspend fun dismissNudge(nudgeId: String) {
        nudgeRepository.dismissNudge(nudgeId)
        nudgeEngine.dismissNudge(nudgeId)
    }
    
    /**
     * Marks a nudge action as taken
     */
    suspend fun takeNudgeAction(nudgeId: String) {
        nudgeRepository.markActionTaken(nudgeId)
        nudgeEngine.markActionTaken(nudgeId)
    }
}

/**
 * Data class containing all information needed to analyze a habit
 */
data class HabitAnalysisData(
    val habitId: Long,
    val habitName: String,
    val stats: HabitStats,
    val lastCompletedDate: LocalDate?,
    val isCompletedToday: Boolean
)
