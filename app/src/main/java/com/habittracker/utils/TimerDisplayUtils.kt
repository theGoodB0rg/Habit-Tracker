package com.habittracker.utils

/**
 * Utility functions for timer display formatting
 * Fixes the common bug where timer displays show incorrect values like "84:55" instead of "1:24:55"
 */
object TimerDisplayUtils {
    
    /**
     * Formats milliseconds into a readable time string
     * @param millis Time in milliseconds
     * @param showHours Whether to always show hours or only when >= 1 hour
     * @return Formatted time string (e.g., "1:24:55" or "24:55")
     */
    fun formatTime(millis: Long, showHours: Boolean = false): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 || showHours -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Formats remaining time with "remaining" suffix
     * @param millis Time in milliseconds
     * @return Formatted time string with "remaining" (e.g., "24:55 remaining")
     */
    fun formatRemainingTime(millis: Long): String {
        return "${formatTime(millis)} remaining"
    }
    
    /**
     * Formats elapsed time 
     * @param millis Time in milliseconds
     * @return Formatted time string (e.g., "24:55")
     */
    fun formatElapsedTime(millis: Long): String {
        return formatTime(millis)
    }
    
    /**
     * Formats time for accessibility
     * @param millis Time in milliseconds
     * @return Human-readable time description for screen readers
     */
    fun formatTimeForAccessibility(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        val parts = mutableListOf<String>()
        if (hours > 0) {
            parts.add("$hours ${if (hours == 1L) "hour" else "hours"}")
        }
        if (minutes > 0) {
            parts.add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add("$seconds ${if (seconds == 1L) "second" else "seconds"}")
        }
        
        return parts.joinToString(", ")
    }
}
