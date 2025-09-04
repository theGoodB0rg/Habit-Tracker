package com.habittracker.data.database.dao

import androidx.room.*
import com.habittracker.data.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for habit-related database operations.
 * Provides methods for CRUD operations on habits.
 */
@Dao
interface HabitDao {
    
    /**
     * Get all habits as a Flow for reactive UI updates
     */
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdDate DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>
    
    /**
     * Get a specific habit by ID
     */
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): HabitEntity?
    
    /**
     * Insert a new habit
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long
    
    /**
     * Insert multiple habits (useful for testing)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)
    
    /**
     * Update an existing habit
     */
    @Update
    suspend fun updateHabit(habit: HabitEntity)
    
    /**
     * Delete a habit (soft delete by marking as inactive)
     */
    @Query("UPDATE habits SET isActive = 0 WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)
    
    /**
     * Hard delete a habit (for testing purposes)
     */
    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun hardDeleteHabit(habitId: Long)
    
    /**
     * Get habits count for analytics
     */
    @Query("SELECT COUNT(*) FROM habits WHERE isActive = 1")
    suspend fun getActiveHabitsCount(): Int
    
    /**
     * Update habit streak and last completed date
     */
    @Query("UPDATE habits SET streakCount = :streakCount, lastCompletedDate = :lastCompletedDate WHERE id = :habitId")
    suspend fun updateHabitStreak(habitId: Long, streakCount: Int, lastCompletedDate: java.time.LocalDate?)

    // Phase UIX-1: timer foundations update helpers
    @Query("UPDATE habits SET timerEnabled = :enabled WHERE id = :habitId")
    suspend fun setTimerEnabled(habitId: Long, enabled: Boolean)

    @Query("UPDATE habits SET customDurationMinutes = :minutes WHERE id = :habitId")
    suspend fun setCustomDurationMinutes(habitId: Long, minutes: Int?)

    @Query("UPDATE habits SET alertProfileId = :profileId WHERE id = :habitId")
    suspend fun setAlertProfile(habitId: Long, profileId: String?)
}
