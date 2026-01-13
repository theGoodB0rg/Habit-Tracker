package com.habittracker.timing

import android.content.Context
import android.content.Intent
import com.habittracker.ui.models.timing.TimerType
import java.time.Duration

/**
 * Tiny controller to start/pause/resume/complete the TimerService and expose pure math utils.
 */
class TimerController(private val context: Context) {

    fun start(habitId: Long, type: TimerType = TimerType.SIMPLE, duration: Duration? = null) {
        val i = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_HABIT_ID, habitId)
            putExtra(TimerService.EXTRA_TIMER_TYPE, type.name)
            duration?.let { putExtra(TimerService.EXTRA_DURATION_MINUTES, it.toMinutes()) }
        }
        context.startForegroundService(i)
    }

    fun pause() = send(TimerService.ACTION_PAUSE)
    fun resume() = send(TimerService.ACTION_RESUME)
    fun complete() = send(TimerService.ACTION_COMPLETE)
    fun extendFiveMinutes() = send(TimerService.ACTION_EXTEND_5M)
    fun addOneMinute() = send(TimerService.ACTION_ADD_1M)
    fun subtractOneMinute() = send(TimerService.ACTION_SUB_1M)
    fun stop() = send(TimerService.ACTION_STOP)

    fun resumeSession(habitId: Long, sessionId: Long) {
        val i = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME_SESSION
            putExtra(TimerService.EXTRA_SESSION_ID, sessionId)
            putExtra(TimerService.EXTRA_HABIT_ID, habitId)
        }
        context.startForegroundService(i)
    }

    private fun send(action: String) {
        val i = Intent(context, TimerService::class.java).apply { this.action = action }
        context.startService(i)
    }

    object TimerMath {
        data class State(
            val startMs: Long,
            val targetMs: Long,
            val pausedAtMs: Long? = null,
            val pausedAccumMs: Long = 0L
        )

        fun remainingMs(nowMs: Long, s: State): Long {
            val effectiveNow = s.pausedAtMs ?: nowMs
            val elapsed = (effectiveNow - s.startMs) - s.pausedAccumMs
            val rem = s.targetMs - elapsed
            return if (rem < 0) 0 else rem
        }

        fun onPause(nowMs: Long, s: State): State = s.copy(pausedAtMs = nowMs)

        fun onResume(nowMs: Long, s: State): State = if (s.pausedAtMs != null) {
            val accum = s.pausedAccumMs + (nowMs - s.pausedAtMs)
            s.copy(pausedAtMs = null, pausedAccumMs = accum)
        } else s
    }
}
