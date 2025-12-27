package com.habittracker.data.repository

import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.database.entity.HabitFrequency
import com.habittracker.domain.engine.HabitManagementEngine
import com.habittracker.domain.model.HabitStats
import com.habittracker.domain.model.HabitStreak
import com.habittracker.ui.models.timing.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for habit data operations.
 * Enhanced for Phase 1 with smart timing capabilities and advanced habit management.
 */
interface HabitRepository {
    // Basic CRUD operations
    fun getAllHabits(): Flow<List<HabitEntity>>
    suspend fun getHabitById(id: Long): HabitEntity?
    suspend fun insertHabit(habit: HabitEntity): Long
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(id: Long)
    suspend fun getActiveHabitsCount(): Int
    
    // Enhanced Phase 2 operations
    suspend fun markHabitAsDone(habitId: Long, date: LocalDate = LocalDate.now(), note: String? = null): HabitStreak
    suspend fun markHabitCompleted(habitId: Long, date: LocalDate = LocalDate.now())
    suspend fun unmarkHabitForDate(habitId: Long, date: LocalDate)
    suspend fun getCurrentStreak(habitId: Long): HabitStreak
    suspend fun getHabitStats(habitId: Long): HabitStats
    suspend fun getHabitsAtRisk(): List<HabitEntity>
    suspend fun getTodayCompletionStatus(): Map<Long, Boolean>
    suspend fun getCompletionsInDateRange(habitId: Long, startDate: LocalDate, endDate: LocalDate): List<LocalDate>
    
    // Phase 1 - Smart Timing Enhancement operations
    
    // Timing features
    suspend fun getHabitTiming(habitId: Long): HabitTiming?
    suspend fun saveHabitTiming(habitId: Long, timing: HabitTiming)
    suspend fun enableTimer(habitId: Long, duration: Duration? = null)
    suspend fun disableTimer(habitId: Long)
    suspend fun setPreferredTime(habitId: Long, time: java.time.LocalTime?)
    suspend fun setEstimatedDuration(habitId: Long, duration: Duration?)
    
    // Timer session management
    suspend fun getActiveTimerSessions(): Flow<List<TimerSession>>
    suspend fun getActiveTimerSession(habitId: Long): TimerSession?
    suspend fun startTimerSession(habitId: Long, timerType: TimerType = TimerType.SIMPLE, duration: Duration? = null): Long
    suspend fun pauseTimerSession(sessionId: Long)
    suspend fun resumeTimerSession(sessionId: Long)
    suspend fun completeTimerSession(sessionId: Long, actualDuration: Duration)
    suspend fun cancelTimerSession(sessionId: Long)
    suspend fun getRecentCompletedTimerSessions(habitId: Long, limit: Int = 14): List<TimerSession>
    
    // Smart suggestions
    suspend fun getSmartSuggestions(habitId: Long): List<SmartSuggestion>
    suspend fun generateSmartSuggestions(habitId: Long): List<SmartSuggestion>
    suspend fun recordSuggestionInteraction(suggestionId: Long, accepted: Boolean)
    suspend fun clearExpiredSuggestions()
    
    // Analytics and metrics
    suspend fun getCompletionMetrics(habitId: Long): Flow<CompletionMetrics?>
    suspend fun recordCompletionMetrics(habitId: Long, completionTime: java.time.LocalTime, duration: Duration? = null, efficiency: Float? = null)
    suspend fun updateHabitAnalytics(habitId: Long)
    
    // Development utilities
    suspend fun insertDummyData()
    suspend fun insertEnhancedDummyData()
    suspend fun insertTimingDummyData() // New for timing features

    // UIX-6: Per-habit alert profile assignment
    suspend fun setAlertProfile(habitId: Long, profileId: String?)
}

/**
 * Implementation of HabitRepository using the HabitManagementEngine.
 * Enhanced with Phase 1 smart timing capabilities.
 * Provides comprehensive habit management with professional-grade streak tracking and timing features.
 */
@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitManagementEngine: HabitManagementEngine,
    private val timingRepository: com.habittracker.data.repository.timing.TimingRepository
) : HabitRepository {
    
    override fun getAllHabits(): Flow<List<HabitEntity>> {
        return habitManagementEngine.getAllHabits()
    }
    
    override suspend fun getHabitById(id: Long): HabitEntity? {
        return habitManagementEngine.getHabitById(id)
    }
    
    override suspend fun insertHabit(habit: HabitEntity): Long {
        return habitManagementEngine.addHabit(habit)
    }
    
    override suspend fun updateHabit(habit: HabitEntity) {
        habitManagementEngine.updateHabit(habit)
    }
    
    override suspend fun deleteHabit(id: Long) {
        habitManagementEngine.deleteHabit(id)
    }
    
    override suspend fun getActiveHabitsCount(): Int {
        return habitManagementEngine.getAllHabits().first().size
    }
    
    override suspend fun markHabitAsDone(habitId: Long, date: LocalDate, note: String?): HabitStreak {
        return habitManagementEngine.markHabitAsDone(habitId, date, note)
    }
    
    override suspend fun markHabitCompleted(habitId: Long, date: LocalDate) {
        habitManagementEngine.markHabitAsDone(habitId, date)
    }
    
    override suspend fun unmarkHabitForDate(habitId: Long, date: LocalDate) {
        habitManagementEngine.unmarkHabitForDate(habitId, date)
    }
    
    override suspend fun getCurrentStreak(habitId: Long): HabitStreak {
        return habitManagementEngine.getCurrentStreak(habitId)
    }
    
    override suspend fun getHabitStats(habitId: Long): HabitStats {
        return habitManagementEngine.getHabitStats(habitId)
    }
    
    override suspend fun getHabitsAtRisk(): List<HabitEntity> {
        return habitManagementEngine.getHabitsAtRisk()
    }
    
    override suspend fun getTodayCompletionStatus(): Map<Long, Boolean> {
        return habitManagementEngine.getTodayCompletionStatus()
    }

    override suspend fun getCompletionsInDateRange(habitId: Long, startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        return habitManagementEngine.getCompletionsInDateRange(habitId, startDate, endDate)
    }
    
    // Phase 1 - Smart Timing Enhancement implementation
    
    // Timing features
    override suspend fun getHabitTiming(habitId: Long): HabitTiming? {
        return timingRepository.getHabitTiming(habitId)
    }
    
    override suspend fun saveHabitTiming(habitId: Long, timing: HabitTiming) {
        timingRepository.saveHabitTiming(habitId, timing)
    }
    
    override suspend fun enableTimer(habitId: Long, duration: Duration?) {
        timingRepository.enableTimer(habitId, duration)
    }
    
    override suspend fun disableTimer(habitId: Long) {
        timingRepository.disableTimer(habitId)
    }
    
    override suspend fun setPreferredTime(habitId: Long, time: java.time.LocalTime?) {
        timingRepository.setPreferredTime(habitId, time)
    }
    
    override suspend fun setEstimatedDuration(habitId: Long, duration: Duration?) {
        timingRepository.setEstimatedDuration(habitId, duration)
    }
    
    // Timer session management
    override suspend fun getActiveTimerSessions(): Flow<List<TimerSession>> {
        return timingRepository.getActiveTimerSessions()
    }
    
    override suspend fun getActiveTimerSession(habitId: Long): TimerSession? {
        return timingRepository.getActiveTimerSession(habitId)
    }
    
    override suspend fun startTimerSession(habitId: Long, timerType: TimerType, duration: Duration?): Long {
        return timingRepository.startTimerSession(habitId, timerType, duration)
    }
    
    override suspend fun pauseTimerSession(sessionId: Long) {
        timingRepository.pauseTimerSession(sessionId)
    }
    
    override suspend fun resumeTimerSession(sessionId: Long) {
        timingRepository.resumeTimerSession(sessionId)
    }
    
    override suspend fun completeTimerSession(sessionId: Long, actualDuration: Duration) {
        timingRepository.completeTimerSession(sessionId, actualDuration)
    }
    
    override suspend fun cancelTimerSession(sessionId: Long) {
        timingRepository.cancelTimerSession(sessionId)
    }

    override suspend fun getRecentCompletedTimerSessions(habitId: Long, limit: Int): List<TimerSession> {
        return timingRepository.getRecentCompletedTimerSessions(habitId, limit)
    }
    
    // Smart suggestions
    override suspend fun getSmartSuggestions(habitId: Long): List<SmartSuggestion> {
        return timingRepository.getSmartSuggestions(habitId)
    }
    
    override suspend fun generateSmartSuggestions(habitId: Long): List<SmartSuggestion> {
        return timingRepository.generateSmartSuggestions(habitId)
    }
    
    override suspend fun recordSuggestionInteraction(suggestionId: Long, accepted: Boolean) {
        timingRepository.recordSuggestionInteraction(suggestionId, accepted)
    }
    
    override suspend fun clearExpiredSuggestions() {
        timingRepository.clearExpiredSuggestions()
    }
    
    // Analytics and metrics
    override suspend fun getCompletionMetrics(habitId: Long): Flow<CompletionMetrics?> {
        return timingRepository.getCompletionMetrics(habitId)
    }
    
    override suspend fun recordCompletionMetrics(habitId: Long, completionTime: java.time.LocalTime, duration: Duration?, efficiency: Float?) {
        timingRepository.recordCompletionMetrics(habitId, completionTime, duration, efficiency)
    }
    
    override suspend fun updateHabitAnalytics(habitId: Long) {
        timingRepository.updateHabitAnalytics(habitId)
    }
    
    override suspend fun insertDummyData() {
        val currentDate = Date()
        val today = LocalDate.now()
        
        val dummyHabits = listOf(
            HabitEntity(
                name = "Drink Water",
                description = "Drink 8 glasses of water daily",
                iconId = android.R.drawable.ic_input_add,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate,
                streakCount = 3,
                lastCompletedDate = today.minusDays(1)
            ),
            HabitEntity(
                name = "Exercise",
                description = "30 minutes of physical activity",
                iconId = android.R.drawable.ic_input_add,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate,
                streakCount = 1,
                lastCompletedDate = today
            ),
            HabitEntity(
                name = "Read Books",
                description = "Read for at least 20 minutes",
                iconId = android.R.drawable.ic_input_add,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate,
                streakCount = 7,
                lastCompletedDate = today
            ),
            HabitEntity(
                name = "Weekly Planning",
                description = "Plan the upcoming week",
                iconId = android.R.drawable.ic_input_add,
                frequency = HabitFrequency.WEEKLY,
                createdDate = currentDate,
                streakCount = 2,
                lastCompletedDate = today.minusDays(3)
            )
        )
        
        // Insert habits and their completion history
        for (habit in dummyHabits) {
            val habitId = habitManagementEngine.addHabit(habit)
            
            // Create realistic completion history
            when (habit.name) {
                "Drink Water" -> {
                    // Completed for last 3 days
                    repeat(3) { day ->
                        habitManagementEngine.markHabitAsDone(habitId, today.minusDays(day.toLong()))
                    }
                }
                "Exercise" -> {
                    // Completed today only
                    habitManagementEngine.markHabitAsDone(habitId, today)
                }
                "Read Books" -> {
                    // Completed for last 7 days
                    repeat(7) { day ->
                        habitManagementEngine.markHabitAsDone(habitId, today.minusDays(day.toLong()))
                    }
                }
                "Weekly Planning" -> {
                    // Completed this week and last week
                    habitManagementEngine.markHabitAsDone(habitId, today.minusDays(3))
                    habitManagementEngine.markHabitAsDone(habitId, today.minusDays(10))
                }
            }
        }
    }
    
    override suspend fun insertEnhancedDummyData() {
        val currentDate = Date()
        val today = LocalDate.now()
        // Keep prebundled habits lightweight: skip seeding historical completions to avoid SlotTable crashes tied to precompleted items.
        val seedCompletions = false
        
        val enhancedHabits = listOf(
            HabitEntity(
                name = "Morning Meditation",
                description = "10 minutes of mindfulness meditation",
                iconId = android.R.drawable.ic_media_pause,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate
            ),
            HabitEntity(
                name = "Workout Session",
                description = "45 minutes strength training or cardio",
                iconId = android.R.drawable.ic_media_play,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate
            ),
            HabitEntity(
                name = "Learning Code",
                description = "Study programming for 30 minutes",
                iconId = android.R.drawable.ic_dialog_info,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate
            ),
            HabitEntity(
                name = "Family Time",
                description = "Quality time with family",
                iconId = android.R.drawable.ic_menu_agenda,
                frequency = HabitFrequency.WEEKLY,
                createdDate = currentDate
            ),
            HabitEntity(
                name = "Budget Review",
                description = "Review monthly expenses and savings",
                iconId = android.R.drawable.ic_menu_month,
                frequency = HabitFrequency.MONTHLY,
                createdDate = currentDate
            )
        )
        
        for (habit in enhancedHabits) {
            val habitId = habitManagementEngine.addHabit(habit)
            if (!seedCompletions) continue

            // Create varied completion patterns for realistic testing (disabled by default)
            when (habit.frequency) {
                HabitFrequency.DAILY -> {
                    val pattern = listOf(1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1)
                    pattern.forEachIndexed { index, completed ->
                        if (completed == 1) {
                            habitManagementEngine.markHabitAsDone(habitId, today.minusDays(index.toLong()))
                        }
                    }
                }
                HabitFrequency.WEEKLY -> {
                    repeat(3) { week ->
                        habitManagementEngine.markHabitAsDone(habitId, today.minusDays((week * 7).toLong()))
                    }
                }
                HabitFrequency.MONTHLY -> {
                    habitManagementEngine.markHabitAsDone(habitId, today.minusDays(30))
                    habitManagementEngine.markHabitAsDone(habitId, today.minusDays(60))
                }
            }
        }
    }
    
    override suspend fun insertTimingDummyData() {
        val habits = habitManagementEngine.getAllHabits().first()
        if (habits.isEmpty()) return
        
        val currentTime = java.time.LocalTime.now()
        
        for (habit in habits.take(3)) { // Add timing data to first 3 habits
            when (habit.name) {
                "Drink Water" -> {
                    val timing = HabitTiming.createWithSchedule(java.time.LocalTime.of(8, 0))
                        .copy(timerEnabled = true, estimatedDuration = Duration.ofMinutes(5))
                    timingRepository.saveHabitTiming(habit.id, timing)
                    
                    // Add some completion metrics
                    for (day in 1..7) {
                        timingRepository.recordCompletionMetrics(
                            habitId = habit.id,
                            completionTime = java.time.LocalTime.of(8, kotlin.random.Random.nextInt(0, 30)),
                            duration = Duration.ofMinutes(kotlin.random.Random.nextLong(3, 8)),
                            efficiency = 0.8f + kotlin.random.Random.nextFloat() * 0.2f
                        )
                    }
                }
                
                "Exercise" -> {
                    val timing = HabitTiming.createWithTimer(Duration.ofMinutes(30))
                        .copy(preferredTime = java.time.LocalTime.of(18, 0), isSchedulingEnabled = true)
                    timingRepository.saveHabitTiming(habit.id, timing)
                    
                    // Generate smart suggestions
                    timingRepository.generateSmartSuggestions(habit.id)
                }
                
                "Read Books" -> {
                    val timing = HabitTiming.createWithSchedule(java.time.LocalTime.of(21, 0))
                        .copy(timerEnabled = true, estimatedDuration = Duration.ofMinutes(20))
                    timingRepository.saveHabitTiming(habit.id, timing)
                    
                    // Add completion metrics for reading
                    for (day in 1..10) {
                        timingRepository.recordCompletionMetrics(
                            habitId = habit.id,
                            completionTime = java.time.LocalTime.of(21, kotlin.random.Random.nextInt(0, 60)),
                            duration = Duration.ofMinutes(kotlin.random.Random.nextLong(15, 35)),
                            efficiency = 0.7f + kotlin.random.Random.nextFloat() * 0.3f
                        )
                    }
                }
            }
        }
    }

    // UIX-6: Per-habit alert profile assignment implementation
    override suspend fun setAlertProfile(habitId: Long, profileId: String?) {
        val existing = habitManagementEngine.getHabitById(habitId) ?: return
        val updated = existing.copy(alertProfileId = profileId)
        habitManagementEngine.updateHabit(updated)
    }
}
