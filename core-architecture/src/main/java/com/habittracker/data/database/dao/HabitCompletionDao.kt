package com.habittracker.data.database.dao

import androidx.room.*
import com.habittracker.data.database.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for habit completion operations
 */
@Dao
interface HabitCompletionDao {
    
    /**
     * Insert a new habit completion
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long
    
    /**
     * Get all completions for a specific habit
     */
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>>
    
    /**
     * Get completions for a habit within a date range
     */
    @Query("""
        SELECT * FROM habit_completions 
        WHERE habitId = :habitId 
        AND completedDate BETWEEN :startDate AND :endDate 
        ORDER BY completedDate DESC
    """)
    suspend fun getCompletionsInDateRange(
        habitId: Long, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<HabitCompletionEntity>
    
    /**
     * Check if habit was completed on a specific date
     */
    @Query("SELECT COUNT(*) > 0 FROM habit_completions WHERE habitId = :habitId AND completedDate = :date")
    suspend fun isHabitCompletedOnDate(habitId: Long, date: LocalDate): Boolean

    @Query("SELECT COUNT(*) > 0 FROM habit_completions WHERE habitId = :habitId AND periodKey = :periodKey")
    suspend fun isHabitCompletedForPeriod(habitId: Long, periodKey: String): Boolean

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND periodKey = :periodKey")
    suspend fun deleteCompletionForPeriod(habitId: Long, periodKey: String)
    
    /**
     * Get the last completion date for a habit
     */
    @Query("SELECT MAX(completedDate) FROM habit_completions WHERE habitId = :habitId")
    suspend fun getLastCompletionDate(habitId: Long): LocalDate?
    
    /**
     * Get total completions count for a habit
     */
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId")
    suspend fun getTotalCompletions(habitId: Long): Int
    
    /**
     * Get completions count for current week
     */
    @Query("""
        SELECT COUNT(*) FROM habit_completions 
        WHERE habitId = :habitId 
        AND completedDate >= :weekStart 
        AND completedDate <= :weekEnd
    """)
    suspend fun getWeeklyCompletions(habitId: Long, weekStart: LocalDate, weekEnd: LocalDate): Int
    
    /**
     * Get completions count for current month
     */
    @Query("""
        SELECT COUNT(*) FROM habit_completions 
        WHERE habitId = :habitId 
        AND completedDate >= :monthStart 
        AND completedDate <= :monthEnd
    """)
    suspend fun getMonthlyCompletions(habitId: Long, monthStart: LocalDate, monthEnd: LocalDate): Int
    
    /**
     * Delete a specific completion
     */
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteCompletion(habitId: Long, date: LocalDate)
    
    /**
     * Delete all completions for a habit
     */
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteAllCompletionsForHabit(habitId: Long)
    
    /**
     * Get all completions for today across all habits
     */
    @Query("SELECT * FROM habit_completions WHERE completedDate = :today")
    suspend fun getTodayCompletions(today: LocalDate): List<HabitCompletionEntity>
}
