package com.habittracker.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {
    
    fun getAvailableIcons(): List<Pair<Int, ImageVector>> {
        return listOf(
            1 to Icons.Filled.FitnessCenter,   // Exercise
            2 to Icons.Filled.LocalDrink,      // Water
            3 to Icons.Filled.MenuBook,        // Reading
            4 to Icons.Filled.DirectionsRun,   // Running
            5 to Icons.Filled.Bedtime,         // Sleep
            6 to Icons.Filled.Restaurant,      // Eating
            7 to Icons.Filled.School,          // Learning
            8 to Icons.Filled.Work,            // Work
            9 to Icons.Filled.Favorite,        // Health
            10 to Icons.Filled.SelfImprovement, // Meditation
            11 to Icons.Filled.MusicNote,      // Music
            12 to Icons.Filled.Brush,          // Art
            13 to Icons.Filled.Code,           // Coding
            14 to Icons.Filled.Language,       // Languages
            15 to Icons.Filled.Savings,        // Money
            16 to Icons.Filled.CleanHands,     // Hygiene
            17 to Icons.Filled.Eco,            // Environment
            18 to Icons.Filled.Group,          // Social
            19 to Icons.Filled.Timer,          // Time management
            20 to Icons.Filled.Star,           // Goals
        )
    }
    
    fun getIconById(iconId: Int): ImageVector {
        return getAvailableIcons().find { it.first == iconId }?.second ?: Icons.Filled.Circle
    }
    
    fun getIconName(iconId: Int): String {
        return when (iconId) {
            1 -> "Exercise"
            2 -> "Water"
            3 -> "Reading"
            4 -> "Running"
            5 -> "Sleep"
            6 -> "Eating"
            7 -> "Learning"
            8 -> "Work"
            9 -> "Health"
            10 -> "Meditation"
            11 -> "Music"
            12 -> "Art"
            13 -> "Coding"
            14 -> "Languages"
            15 -> "Money"
            16 -> "Hygiene"
            17 -> "Environment"
            18 -> "Social"
            19 -> "Time"
            20 -> "Goals"
            else -> "General"
        }
    }
}
