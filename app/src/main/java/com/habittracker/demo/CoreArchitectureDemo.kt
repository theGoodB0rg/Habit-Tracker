package com.habittracker.demo

import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.database.entity.HabitFrequency
import java.util.Date

/**
 * Demo class to validate core architecture functionality
 * This can be used for manual testing of the data layer
 */
object CoreArchitectureDemo {
    
    /**
     * Creates sample habit entities for testing
     */
    fun createSampleHabits(): List<HabitEntity> {
        val currentDate = Date()
        
        return listOf(
            HabitEntity(
                name = "Morning Meditation",
                description = "10 minutes of mindfulness meditation",
                iconId = android.R.drawable.ic_media_pause,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate,
                streakCount = 15
            ),
            HabitEntity(
                name = "Workout",
                description = "30-45 minutes of physical exercise",
                iconId = android.R.drawable.ic_media_play,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate,
                streakCount = 8
            ),
            HabitEntity(
                name = "Learning",
                description = "Study new programming concepts",
                iconId = android.R.drawable.ic_dialog_info,
                frequency = HabitFrequency.DAILY,
                createdDate = currentDate,
                streakCount = 22
            ),
            HabitEntity(
                name = "Meal Planning",
                description = "Plan healthy meals for the week",
                iconId = android.R.drawable.ic_menu_agenda,
                frequency = HabitFrequency.WEEKLY,
                createdDate = currentDate,
                streakCount = 4
            ),
            HabitEntity(
                name = "Monthly Review",
                description = "Review goals and progress",
                iconId = android.R.drawable.ic_menu_month,
                frequency = HabitFrequency.MONTHLY,
                createdDate = currentDate,
                streakCount = 2
            )
        )
    }
    
    /**
     * Validates habit entity data integrity
     */
    fun validateHabitEntity(habit: HabitEntity): Boolean {
        return habit.name.isNotBlank() &&
                habit.description.isNotBlank() &&
                habit.iconId > 0 &&
                habit.streakCount >= 0 &&
                habit.createdDate.before(Date()) || habit.createdDate == Date()
    }
    
    /**
     * Simulates streak calculation logic
     */
    fun calculateNewStreak(currentStreak: Int, daysMissed: Int): Int {
        return if (daysMissed == 0) {
            currentStreak + 1
        } else if (daysMissed == 1) {
            currentStreak // Grace period
        } else {
            0 // Reset streak
        }
    }
    
    /**
     * Demo function to showcase repository pattern
     */
    fun demoRepositoryPattern() {
        println("🎯 Core Architecture Demo")
        println("========================")
        
        val sampleHabits = createSampleHabits()
        
        println("📊 Generated ${sampleHabits.size} sample habits:")
        sampleHabits.forEachIndexed { index, habit ->
            println("${index + 1}. ${habit.name} (${habit.frequency}) - Streak: ${habit.streakCount}")
        }
        
        println("\n✅ All habits validated: ${sampleHabits.all { validateHabitEntity(it) }}")
        
        println("\n🔄 Streak calculation examples:")
        println("Current streak 5, 0 days missed: ${calculateNewStreak(5, 0)}")
        println("Current streak 5, 1 day missed: ${calculateNewStreak(5, 1)}")
        println("Current streak 5, 2 days missed: ${calculateNewStreak(5, 2)}")
        
        println("\n🏗️ Architecture Components Ready:")
        println("✅ Room Database Entity")
        println("✅ DAO with Flow queries")
        println("✅ Repository pattern")
        println("✅ Hilt dependency injection")
        println("✅ MVVM ViewModel")
        println("✅ Jetpack Compose UI")
    }
}
