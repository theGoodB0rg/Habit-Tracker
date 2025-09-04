package com.habittracker.core

import java.time.LocalDate

/**
 * Widget-specific data class for habit information
 * Optimized for widget display and interaction
 */
data class HabitWidgetData(
    val id: Long,
    val name: String,
    val description: String = "",
    val icon: Int = 0, // Will use default icon resource from widget module
    val isCompleted: Boolean = false,
    val currentStreak: Int = 0,
    val priority: Int = 0,
    val category: String = "",
    val color: Int = 0,
    val frequency: String = "DAILY",
    val lastCompletedDate: LocalDate? = null
) {
    /**
     * Helper function to get display streak with emoji
     */
    fun getDisplayStreak(): String = "🔥$currentStreak"
    
    /**
     * Helper function to get streak display (alias for compatibility)
     */
    fun getStreakDisplay(): String = getDisplayStreak()
    
    /**
     * Helper function to determine if this is a high-priority habit
     */
    fun isHighPriority(): Boolean = priority <= 3
    
    /**
     * Helper function to get accessibility description
     */
    fun getAccessibilityDescription(): String {
        val status = if (isCompleted) "completed" else "not completed"
        return "$name, $status, $currentStreak day streak"
    }
    
    /**
     * Get accessibility description for completion status
     */
    fun getCompletionAccessibilityText(): String = 
        if (isCompleted) "Mark $name as not done" 
        else "Mark $name as done"
    
    /**
     * Get display name with proper truncation for widget
     */
    fun getDisplayName(maxLength: Int = 25): String = 
        if (name.length > maxLength) "${name.take(maxLength - 3)}..." else name
}
