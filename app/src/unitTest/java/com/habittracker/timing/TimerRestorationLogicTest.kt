package com.habittracker.timing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class TimerRestorationLogicTest {

    @Test
    fun `time shift strategy correctly preserves active duration`() {
        // SCENARIO:
        // Start at 12:00 (0).
        // Run until 12:25 (25 min).
        // Pause at 12:25.
        // Wait until 13:25 (60 min pause).
        // Resume at 13:25.
        
        val originalStartMs = 0L
        val pausedAtMs = Duration.ofMinutes(25).toMillis() // 25 mins in
        val activeDurationWhenPaused = pausedAtMs - originalStartMs
        
        val resumeAtMs = pausedAtMs + Duration.ofMinutes(60).toMillis() // 1h 25m from start time
        val pauseDuration = resumeAtMs - pausedAtMs
        
        // LOGIC FIX: Shift start time forward by the pause duration
        val newStartMs = originalStartMs + pauseDuration
        
        // VERIFY: now - newStart should equal the original active duration (25m)
        val elapsedWithShift = resumeAtMs - newStartMs
        
        assertEquals("Time shift should preserve exact active duration", 
            activeDurationWhenPaused, 
            elapsedWithShift
        )
        
        // VERIFY: The new start time is effectively 1:00 (original 12:00 + 60m)
        assertEquals("New start time should be shifted by pause duration", 
            Duration.ofMinutes(60).toMillis(), 
            newStartMs
        )
    }
}
