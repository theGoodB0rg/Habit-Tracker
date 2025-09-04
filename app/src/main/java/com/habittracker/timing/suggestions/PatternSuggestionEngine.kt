package com.habittracker.timing.suggestions

import java.time.LocalTime

/**
 * Phase 7: Minimal pattern-based suggestion engine.
 *
 * - bestTimeByFrequency: returns the most frequent completion time bucket (hour of day)
 * - optimalDurationMinutes: returns the mode of successful session durations when data is reliable
 */
class PatternSuggestionEngine {
    data class TimeFrequency(val bucketIndex: Int, val count: Int)

    /**
     * Returns the most frequent completion time bucket using 30-minute granularity (48 buckets per day).
     * The returned LocalTime is the bucket start (minute = 0 or 30).
     */
    fun bestTimeByFrequency(times: List<LocalTime>): LocalTime? {
        if (times.isEmpty()) return null
        val buckets = IntArray(48)
        fun bucketIndex(t: LocalTime): Int = (t.hour * 2) + if (t.minute >= 30) 1 else 0
        times.forEach { t -> buckets[bucketIndex(t)]++ }
        val best = buckets.indices.maxByOrNull { buckets[it] } ?: return null
        val hour = best / 2
        val minute = if (best % 2 == 0) 0 else 30
        return LocalTime.of(hour, minute)
    }

    fun optimalDurationMinutes(successfulDurationsMinutes: List<Int>): Int? {
        if (successfulDurationsMinutes.isEmpty()) return null
        val freq = successfulDurationsMinutes.groupingBy { it }.eachCount()
        val (mode, count) = freq.maxByOrNull { it.value } ?: return null
        // Require basic reliability: at least 3 samples and mode seen twice
        return if (successfulDurationsMinutes.size >= 3 && count >= 2) mode else null
    }
}
