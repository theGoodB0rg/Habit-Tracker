package com.habittracker.timing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.data.preferences.TimingPreferences
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.timing.alert.AlertChannels
import com.habittracker.R
import com.habittracker.timing.alert.AlertEngine
import com.habittracker.timing.alert.soundPackById
import com.habittracker.timing.alert.TimerSoundPack
import android.media.RingtoneManager
import com.habittracker.ui.models.timing.TimerType
import com.habittracker.data.database.HabitDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject
import android.util.Log
import com.habittracker.MainActivity
import com.habittracker.analytics.domain.usecases.TrackTimerEventUseCase

/**
 * Minimal foreground service emitting 1s ticks and bridging to TimingRepository.
 * Phase 5 — Timer runtime (service + ticker).
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var timingRepository: TimingRepository
    @Inject lateinit var timingPreferencesRepository: TimingPreferencesRepository
    @Inject lateinit var alertEngine: AlertEngine
    @Inject lateinit var trackTimerEvent: TrackTimerEventUseCase

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickJob: Job? = null
    private var alertListenerJob: Job? = null
    private var prefsJob: Job? = null
    private var lastNonFinalSoundAt: Long = 0L
    private var activeSoundPack: TimerSoundPack = soundPackById("default")
    private var latestPrefs: TimingPreferences = TimingPreferences()
    private var postedHeadsUp: Boolean = false

    // Runtime state
    private var sessionId: Long? = null
    private var habitId: Long = 0L
    private var targetDurationMs: Long = 0L
    private var startMs: Long = 0L
    private var pausedAtMs: Long? = null
    private var pausedAccumMs: Long = 0L
    // Sum of elapsed time across finished segments (Pomodoro)
    private var cumulativeElapsedMs: Long = 0L
    // Phase 5: Pomodoro runtime state
    private var currentTimerType: com.habittracker.ui.models.timing.TimerType = com.habittracker.ui.models.timing.TimerType.SIMPLE
    private var isInBreakSegment: Boolean = false
    private var pomodoroWorkMs: Long = java.time.Duration.ofMinutes(25).toMillis()
    private var pomodoroBreakMs: Long = java.time.Duration.ofMinutes(5).toMillis()
    private var pomodoroCycleCount: Int = 0
    // Phase UIX-2 alert scheduling
    private var alertSchedule: List<AlertPoint> = emptyList()
    private var nextAlertIndex: Int = 0
    // Phase UIX-11: Overtime detection state
    private var overtimeStartedAt: Long? = null
    private var overtimeNudged: Boolean = false
    // Phase 2
    private var autoCompleteOnTarget: Boolean = false
    // Phase 2: Gentle nudge before target
    private var gentleNudgePosted: Boolean = false
    // Analytics: mark when we completed to avoid logging discard on stop
    private var completedThisSession: Boolean = false
    // Debouncing for notification/widget actions to prevent double-tap bugs
    private var lastActionTime: Long = 0L
    private var lastActionType: String? = null
    private val actionDebounceMs = 500L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        // Collect preferences & alert events (Phase UIX-3 delivery channels)
    prefsJob = scope.launch {
            timingPreferencesRepository.preferences().collect { p ->
        latestPrefs = p
                activeSoundPack = soundPackById(p.selectedSoundPackId)
            }
        }
        alertListenerJob = scope.launch {
            TimerAlertBus.events.collect { evt ->
                val now = System.currentTimeMillis()
                val decision = TimerAlertGating.decide(
                    prefs = latestPrefs,
                    eventType = evt.type,
                    lastNonFinalAt = lastNonFinalSoundAt,
                    now = now,
                    soundPack = activeSoundPack
                )
                if (decision.updateLastNonFinal) lastNonFinalSoundAt = now
                if (decision.systemFinalNotification) {
                    postSystemFinalAlertNotification()
                }
                if (!decision.playAny) return@collect
                val vol = (latestPrefs.soundMasterVolumePercent / 100f).coerceIn(0f,1f)
                val pitch = if (latestPrefs.enableToneVariation) when (evt.type) {
                    AlertType.START -> 1.06f
                    AlertType.MIDPOINT -> 1.0f
                    AlertType.PROGRESS -> 1.03f
                    AlertType.FINAL -> 0.94f
                } else 1f
                alertEngine.playAlert(
                    evt.type,
                    AlertChannels(
                        sound = decision.playSound,
                        haptics = decision.playHaptics,
                        spokenText = decision.spokenText,
                        rawResId = decision.rawResId,
                        volume = vol,
                        pitchRate = pitch
                    )
                )
            }
        }

        // Phase 3: Robust process death recovery — if a session is active, restore runtime state
        scope.launch {
            try {
                val active = timingRepository.listActiveTimerSessions().firstOrNull()
                if (active != null) {
                    sessionId = active.id
                    habitId = active.habitId
                    currentTimerType = try { com.habittracker.ui.models.timing.TimerType.valueOf(active.type.name) } catch (_: Exception) { com.habittracker.ui.models.timing.TimerType.SIMPLE }
                    startMs = active.startTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                        ?: System.currentTimeMillis()
                    pausedAtMs = active.pausedTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    // Fix: Reset pausedAccumMs for new sessions to prevent timer display bugs
                    pausedAccumMs = 0L

                    val timing = timingRepository.getHabitTiming(habitId)
                    val targetMinutes = timing?.estimatedDuration?.toMinutes() ?: DEFAULT_MINUTES
                    targetDurationMs = java.time.Duration.ofMinutes(targetMinutes).toMillis()
                    autoCompleteOnTarget = timing?.autoCompleteOnTarget == true

                    startForeground(NOTIFICATION_ID, buildNotification(remainingMs(System.currentTimeMillis())))
                    alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
                    nextAlertIndex = 0
                    postedHeadsUp = false
                    gentleNudgePosted = false
                    startTicker()
                }
            } catch (_: Exception) { /* best-effort recovery */ }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        // Apply debouncing for user-initiated actions (prevents double-tap bugs)
        if (action in DEBOUNCED_ACTIONS) {
            val now = System.currentTimeMillis()
            if (action == lastActionType && (now - lastActionTime) < actionDebounceMs) {
                Log.d("TimerService", "Debounced duplicate action: $action (${now - lastActionTime}ms)")
                return START_STICKY
            }
            lastActionTime = now
            lastActionType = action
        }
        
    when (action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_RESUME_SESSION -> handleResumeSpecific(intent)
            ACTION_COMPLETE -> handleComplete()
            ACTION_EXTEND_5M -> handleExtendFive()
            ACTION_ADD_1M -> handleAddOneMinute()
            ACTION_SUB_1M -> handleSubtractOneMinute()
            ACTION_STOP -> stopSelfSafely()
        }
    // Phase 3: Prefer sticky restart so the system can recreate the service if killed
    return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        val durationMin = intent.getLongExtra(EXTRA_DURATION_MINUTES, -1L)
    val type = intent.getStringExtra(EXTRA_TIMER_TYPE)?.let {
            runCatching { TimerType.valueOf(it) }.getOrNull()
        } ?: TimerType.SIMPLE
    currentTimerType = type

        scope.launch {
            try {
                // Capture currently active sessions BEFORE starting to detect auto-paused ones
                val previouslyActive = try { timingRepository.listActiveTimerSessions() } catch (e: Exception) { emptyList() }
                val session = timingRepository.getActiveTimerSession(habitId)
                if (session?.isRunning == true && tickJob?.isActive == true) {
                    sessionId = session.id
                    currentTimerType = session.type
                    return@launch
                }
                val sid = if (session == null) {
                    timingRepository.startTimerSession(
                        habitId,
                        type,
                        duration = durationMin.takeIf { it > 0 }?.let { Duration.ofMinutes(it) }
                    )
                } else session.id
                sessionId = sid
                if (session != null) {
                    currentTimerType = session.type
                }

                // Determine which sessions got auto-paused by repository enforcement and announce them
                val nowActive = try { timingRepository.listActiveTimerSessions() } catch (e: Exception) { emptyList() }
                val paused = previouslyActive.filter { prev ->
                    nowActive.none { cur -> cur.id == prev.id && cur.isRunning }
                }
                paused.forEach { p ->
                    TimerBus.emit(TimerEvent.AutoPaused(pausedSessionId = p.id, pausedHabitId = p.habitId))
                }

                // Initialize ticker state
                val timing = timingRepository.getHabitTiming(habitId)
                val targetMinutes = durationMin.takeIf { it > 0 }
                    ?: timing?.estimatedDuration?.toMinutes()
                    ?: DEFAULT_MINUTES
                targetDurationMs = Duration.ofMinutes(targetMinutes).toMillis()
                // Phase 5: capture Pomodoro break duration from session if available
                if (currentTimerType == TimerType.POMODORO) {
                    pomodoroWorkMs = targetDurationMs
                    val br = session?.breaks?.firstOrNull()?.duration?.toMinutes()?.toLong()
                    pomodoroBreakMs = java.time.Duration.ofMinutes(br ?: 5L).toMillis()
                    isInBreakSegment = false
                } else {
                    isInBreakSegment = false
                }
                autoCompleteOnTarget = timing?.autoCompleteOnTarget == true
                // Try restoring from existing active session times if available
                if (session != null && session.startTime != null) {
                    startMs = session.startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    pausedAtMs = session.pausedTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    // Fix: Reset pausedAccumMs for resumed sessions to prevent timer display bugs  
                    pausedAccumMs = 0L
                    cumulativeElapsedMs = 0L
                } else {
                    startMs = System.currentTimeMillis()
                    pausedAtMs = null
                    pausedAccumMs = 0L
                    cumulativeElapsedMs = 0L
                }

                startForeground(NOTIFICATION_ID, buildNotification(remainingMs(System.currentTimeMillis())))
                // Build alert schedule (Phase UIX-2) using profile thresholds
                alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
                nextAlertIndex = 0
                postedHeadsUp = false
                gentleNudgePosted = false
                startTicker()
                // Emit start with auto-complete flag for UI awareness
                TimerBus.emit(TimerEvent.Started(sid, habitId, targetDurationMs, autoCompleteOnTarget))
                // Analytics: timer_start
                scope.launch { trackTimerEvent("timer_start", habitId = habitId, sessionId = sid, extra = mapOf("type" to currentTimerType.name, "targetMs" to targetDurationMs)) }
                emitDueAlerts(elapsedMs = 0L) // emit START immediately
            } catch (e: Exception) {
                val msg = "Timer start failed"
                updateErrorNotification(msg)
                TimerBus.emit(TimerEvent.Error(habitId = habitId, message = msg))
                stopSelfSafely()
            }
        }
    }

    private fun handlePause() {
        val sid = sessionId ?: return
        if (pausedAtMs == null) {
            pausedAtMs = System.currentTimeMillis()
            scope.launch {
                try {
                    timingRepository.pauseTimerSession(sid)
                    TimerBus.emit(TimerEvent.Paused(sid, habitId))
                    // Analytics: timer_pause
                    scope.launch { trackTimerEvent("timer_pause", habitId = habitId, sessionId = sid) }
                } catch (e: Exception) {
                    val msg = "Pause failed"
                    updateErrorNotification(msg)
                    TimerBus.emit(TimerEvent.Error(habitId = habitId, message = msg))
                }
            }
        }
    }

    private fun handleResume() {
        val sid = sessionId ?: return
        pausedAtMs?.let { pausedAt ->
            pausedAccumMs += (System.currentTimeMillis() - pausedAt)
            pausedAtMs = null
            scope.launch {
                try {
                    timingRepository.resumeTimerSession(sid)
                    TimerBus.emit(TimerEvent.Resumed(sid, habitId))
                    // Analytics: timer_resume
                    scope.launch { trackTimerEvent("timer_resume", habitId = habitId, sessionId = sid) }
                } catch (e: Exception) {
                    val msg = "Resume failed"
                    updateErrorNotification(msg)
                    TimerBus.emit(TimerEvent.Error(habitId = habitId, message = msg))
                }
            }
        }
    }

    private fun handleResumeSpecific(intent: Intent) {
        val resumeSid = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (resumeSid <= 0L) return
        scope.launch {
            try {
                val session = timingRepository.getTimerSessionById(resumeSid) ?: return@launch
                // Stop any current ticker and switch runtime state to this session
                tickJob?.cancel()
                sessionId = session.id
                habitId = session.habitId
                currentTimerType = session.type
                startMs = session.startTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    ?: System.currentTimeMillis()
                pausedAtMs = session.pausedTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                pausedAccumMs = session.actualDuration.toMillis()

                val timing = timingRepository.getHabitTiming(habitId)
                val targetMinutes = timing?.estimatedDuration?.toMinutes() ?: DEFAULT_MINUTES
                targetDurationMs = java.time.Duration.ofMinutes(targetMinutes).toMillis()
                autoCompleteOnTarget = timing?.autoCompleteOnTarget == true

                startForeground(NOTIFICATION_ID, buildNotification(remainingMs(System.currentTimeMillis())))
                alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
                nextAlertIndex = 0
                postedHeadsUp = false
                gentleNudgePosted = false
                cumulativeElapsedMs = 0L
                startTicker()

                timingRepository.resumeTimerSession(resumeSid)
                TimerBus.emit(TimerEvent.Resumed(resumeSid, habitId))
                // Analytics: timer_resume
                scope.launch { trackTimerEvent("timer_resume", habitId = habitId, sessionId = resumeSid, extra = mapOf("resumeSpecific" to true)) }
            } catch (e: Exception) {
                val msg = "Resume session failed"
                updateErrorNotification(msg)
                TimerBus.emit(TimerEvent.Error(habitId = habitId, message = msg))
            }
        }
    }

    private fun handleComplete() {
        val sid = sessionId ?: return
        scope.launch {
            try {
                val remaining = remainingMs(System.currentTimeMillis())
                val totalMs = cumulativeElapsedMs + elapsedMs(System.currentTimeMillis())
                val actual = Duration.ofMillis(totalMs.coerceAtLeast(0L))
                timingRepository.completeTimerSession(sid, actual)
                TimerBus.emit(TimerEvent.Completed(sid, habitId))
                // Analytics: timer_done; mark autoComplete if applicable
                val auto = autoCompleteOnTarget && remaining <= 0L
                completedThisSession = true
                scope.launch { trackTimerEvent("timer_done", habitId = habitId, sessionId = sid, extra = mapOf("autoComplete" to auto, "actualMs" to actual.toMillis())) }
                if (auto) {
                    scope.launch { trackTimerEvent("timer_auto_complete", habitId = habitId, sessionId = sid) }
                }
                cancelHeadsUpIfAny()
                stopSelfSafely()
            } catch (e: Exception) {
                val msg = "Complete failed"
                updateErrorNotification(msg)
                TimerBus.emit(TimerEvent.Error(habitId = habitId, message = msg))
            }
        }
    }

    private fun handleExtendFive() {
        val sid = sessionId ?: return
    val addedMs = Duration.ofMinutes(5).toMillis()
        targetDurationMs += addedMs
        // Rebuild full schedule for new target and reposition pointer after elapsed
        alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
        val elapsed = elapsedMs(System.currentTimeMillis())
        nextAlertIndex = alertSchedule.indexOfFirst { it.triggerElapsedMs > elapsed }
            .let { if (it == -1) alertSchedule.size else it }
        scope.launch { 
            TimerBus.emit(TimerEvent.Extended(sid, habitId, addedMs, targetDurationMs))
            // Analytics: timer_extend
            trackTimerEvent("timer_extend", habitId = habitId, sessionId = sid, extra = mapOf("deltaMs" to addedMs, "targetMs" to targetDurationMs))
        }
        updateNotification(remainingMs(System.currentTimeMillis()))
    }

    private fun handleAddOneMinute() {
        val sid = sessionId ?: return
    val addedMs = Duration.ofMinutes(1).toMillis()
        targetDurationMs += addedMs
        alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
        val elapsed = elapsedMs(System.currentTimeMillis())
        nextAlertIndex = alertSchedule.indexOfFirst { it.triggerElapsedMs > elapsed }
            .let { if (it == -1) alertSchedule.size else it }
        scope.launch { 
            TimerBus.emit(TimerEvent.Extended(sid, habitId, addedMs, targetDurationMs))
            trackTimerEvent("timer_add_minute", habitId = habitId, sessionId = sid, extra = mapOf("deltaMs" to addedMs, "targetMs" to targetDurationMs))
        }
        updateNotification(remainingMs(System.currentTimeMillis()))
    }

    private fun handleSubtractOneMinute() {
        val sid = sessionId ?: return
    val subMs = Duration.ofMinutes(1).toMillis()
        targetDurationMs = (targetDurationMs - subMs).coerceAtLeast(60_000L)
        alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
        val elapsed = elapsedMs(System.currentTimeMillis())
        nextAlertIndex = alertSchedule.indexOfFirst { it.triggerElapsedMs > elapsed }
            .let { if (it == -1) alertSchedule.size else it }
        scope.launch { 
            TimerBus.emit(TimerEvent.Extended(sid, habitId, -subMs, targetDurationMs))
            trackTimerEvent("timer_sub_minute", habitId = habitId, sessionId = sid, extra = mapOf("deltaMs" to -subMs, "targetMs" to targetDurationMs))
        }
        updateNotification(remainingMs(System.currentTimeMillis()))
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val remaining = remainingMs(now)
                val sid = sessionId ?: continue
                TimerBus.emit(TimerEvent.Tick(sid, habitId, remaining))
                // Only emit alerts when not paused
                if (pausedAtMs == null) {
                    val elapsed = elapsedMs(now)
                    emitDueAlerts(elapsed)
                }
                updateNotification(remaining)
                // Phase 2: Gentle nudge before target (one-time, when auto-complete is enabled)
                if (autoCompleteOnTarget && !gentleNudgePosted && pausedAtMs == null && remaining in 1..120_000L) {
                    postGentleNudgeNotification(remaining)
                    val sidSafe = sessionId
                    if (sidSafe != null) {
                        TimerBus.emit(TimerEvent.NearTarget(sidSafe, habitId, remaining))
                    }
                    gentleNudgePosted = true
                }
                // Phase 2: Auto-complete at target when enabled
                if (remaining <= 0L && pausedAtMs == null && sessionId != null) {
                    if (currentTimerType == TimerType.POMODORO) {
                        advancePomodoroSegment(now)
                        continue
                    } else if (autoCompleteOnTarget) {
                        handleComplete()
                        return@launch
                    }
                }
                // Phase UIX-7: Optional heads-up notification for final 10s
                if (pausedAtMs == null && !postedHeadsUp && latestPrefs.enableHeadsUpFinal && remaining in 1..10_000) {
                    postHeadsUpFinalNotification(remaining)
                    postedHeadsUp = true
                }
                // Phase UIX-11: Overtime (+1m) detection and one-time nudge event
                if (remaining <= 0L) {
                    val nowMs = System.currentTimeMillis()
                    if (overtimeStartedAt == null) {
                        overtimeStartedAt = nowMs
                    }
                    val started = overtimeStartedAt
                    if (started != null && !overtimeNudged) {
                        val overMs = nowMs - started
                        if (overMs >= 60_000L) {
                            val sidSafe = sessionId
                            if (sidSafe != null) {
                                TimerBus.emit(TimerEvent.Overtime(sidSafe, habitId, overMs))
                                overtimeNudged = true // one nudge cap per session
                            }
                        }
                    }
                    // Keep service running to allow user actions (complete/extend)
                }
            }
        }
    }

    private fun advancePomodoroSegment(now: Long) {
        val sid = sessionId ?: return
        if (!isInBreakSegment) {
            // Transition from focus -> break
            cumulativeElapsedMs += elapsedMs(now).coerceAtLeast(0L)
            pomodoroCycleCount += 1
            isInBreakSegment = true
            startMs = now
            pausedAtMs = null
            pausedAccumMs = 0L
            targetDurationMs = pomodoroBreakMs
            alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
            nextAlertIndex = 0
            postedHeadsUp = false
            gentleNudgePosted = false
            // Let UI know segment changed
            TimerBus.emit(TimerEvent.SegmentChanged(sid, habitId, inBreak = true, cycleCount = pomodoroCycleCount))
            updateNotification(remainingMs(now))
        } else {
            // Transition from break -> next focus
            cumulativeElapsedMs += elapsedMs(now).coerceAtLeast(0L)
            isInBreakSegment = false
            startMs = now
            pausedAtMs = null
            pausedAccumMs = 0L
            targetDurationMs = pomodoroWorkMs
            alertSchedule = buildScheduleForHabit(habitId, targetDurationMs)
            nextAlertIndex = 0
            postedHeadsUp = false
            gentleNudgePosted = false
            TimerBus.emit(TimerEvent.SegmentChanged(sid, habitId, inBreak = false, cycleCount = pomodoroCycleCount))
            updateNotification(remainingMs(now))
        }
    }

    private fun remainingMs(nowMs: Long): Long {
        val effectiveNow = pausedAtMs ?: nowMs
        val elapsed = (effectiveNow - startMs) - pausedAccumMs
        val rem = targetDurationMs - elapsed
        return if (rem < 0) 0 else rem
    }

    private fun elapsedMs(nowMs: Long): Long {
        val effectiveNow = pausedAtMs ?: nowMs
        return (effectiveNow - startMs) - pausedAccumMs
    }

    private fun emitDueAlerts(elapsedMs: Long) {
        val sid = sessionId ?: return
        while (nextAlertIndex < alertSchedule.size) {
            val point = alertSchedule[nextAlertIndex]
            if (elapsedMs >= point.triggerElapsedMs) {
                val remaining = (targetDurationMs - elapsedMs).coerceAtLeast(0)
                val event = TimerAlertEvent(
                    habitId = habitId,
                    sessionId = sid,
                    percent = point.percent,
                    remainingMs = remaining,
                    type = point.type,
                    emittedAt = System.currentTimeMillis()
                )
                TimerAlertBus.emit(event)
                Log.d("TimerService", "Alert fired percent=${point.percent} type=${point.type}")
                nextAlertIndex++
            } else break
        }
    }

    private fun buildScheduleForHabit(habitId: Long, targetDurationMs: Long): List<AlertPoint> {
        // If feature disabled globally, return only start & final
        if (!TimerFeatureFlags.enableAlertScheduling) {
            return AlertScheduleBuilder.build(targetDurationMs, listOf(0,100))
        }
        return try {
            // Access DB directly for profile (light read) — could be moved to repository
            val db = HabitDatabase.getInstance(applicationContext).openHelper.readableDatabase
            // Read habit row (alertProfileId)
            var profileId: String? = null
            db.query("SELECT alertProfileId FROM habits WHERE id = ?", arrayOf(habitId.toString())).use { c ->
                if (c.moveToFirst()) profileId = c.getString(0)
            }
            val targetProfile = profileId ?: DEFAULT_PROFILE_ID
            var thresholdsJson: String? = null
            db.query("SELECT thresholdsJson FROM timer_alert_profiles WHERE id = ?", arrayOf(targetProfile)).use { c ->
                if (c.moveToFirst()) thresholdsJson = c.getString(0)
            }
            val parsed = thresholdsJson
                ?.removePrefix("[")
                ?.removeSuffix("]")
                ?.split(',')
                ?.mapNotNull { it.trim().toIntOrNull() }
                .takeIf { !it.isNullOrEmpty() } ?: DEFAULT_FOCUS_THRESHOLDS
            AlertScheduleBuilder.build(targetDurationMs, parsed)
        } catch (e: Exception) {
            Log.w("TimerService", "Failed to load profile thresholds; fallback", e)
            AlertScheduleBuilder.build(targetDurationMs, DEFAULT_FOCUS_THRESHOLDS)
        }
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        tickJob?.cancel()
        tickJob = null
    alertListenerJob?.cancel(); alertListenerJob = null
    prefsJob?.cancel(); prefsJob = null
    cancelHeadsUpIfAny()
        val sid = sessionId
        val hid = habitId
    if (sid != null && !completedThisSession) {
            // Consider this a discard if not completed
            scope.launch { trackTimerEvent("timer_discard", habitId = hid, sessionId = sid) }
        }
        sessionId = null
    // Reset overtime state
    overtimeStartedAt = null
    overtimeNudged = false
    gentleNudgePosted = false
    completedThisSession = false
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            val finalChannel = NotificationChannel(
                CHANNEL_ID_FINAL,
                "Timer Final Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                description = "Final timer completion alerts"
            }
            nm.createNotificationChannel(finalChannel)
            // Heads-up channel for last 10s (opt-in via preferences)
            val headsUp = NotificationChannel(
                CHANNEL_ID_HEADS_UP,
                "Timer Heads-Up",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority heads-up alert during final 10 seconds"
                enableVibration(true)
            }
            nm.createNotificationChannel(headsUp)
        }
    }

    private fun buildNotification(remainingMs: Long, errorText: String? = null): Notification {
        val paused = pausedAtMs != null
        val totalSeconds = (remainingMs / 1000)
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        
        val timeText = if (hours > 0) {
            String.format("%d:%02d:%02d remaining", hours, mins, secs)
        } else {
            String.format("%d:%02d remaining", mins, secs)
        }
        
        val baseText = if (errorText != null) errorText else if (paused) getString(R.string.notif_timer_paused) else timeText
        val title = getString(R.string.app_name)
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (paused) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(baseText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainContentPendingIntent())

        // Actions
        if (errorText == null) {
            if (paused) {
                builder.addAction(android.R.drawable.ic_media_play, getString(R.string.action_resume), servicePendingIntent(ACTION_RESUME))
            } else {
                builder.addAction(android.R.drawable.ic_media_pause, getString(R.string.action_pause), servicePendingIntent(ACTION_PAUSE))
            }
            builder.addAction(android.R.drawable.ic_input_add, getString(R.string.action_extend_5m), servicePendingIntent(ACTION_EXTEND_5M))
            // Quick +/-1m controls (Phase 2)
            builder.addAction(0, getString(R.string.action_add_1m), servicePendingIntent(ACTION_ADD_1M))
            builder.addAction(0, getString(R.string.action_sub_1m), servicePendingIntent(ACTION_SUB_1M))
            builder.addAction(android.R.drawable.checkbox_on_background, getString(R.string.action_complete), servicePendingIntent(ACTION_COMPLETE))
        }
        return builder.build()
    }

    private fun updateNotification(remainingMs: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(remainingMs))
    }

    private fun updateErrorNotification(message: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(remainingMs(System.currentTimeMillis()), errorText = message))
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply { this.action = action }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getService(this, action.hashCode(), intent, flags)
    }

    private fun mainContentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    companion object {
        const val CHANNEL_ID = "habit_timer_channel"
    const val CHANNEL_ID_FINAL = "habit_timer_final_channel"
    const val CHANNEL_ID_HEADS_UP = "habit_timer_heads_up_channel"
        const val NOTIFICATION_ID = 4242
    const val FINAL_ALERT_NOTIFICATION_ID = 4243
    const val HEADS_UP_NOTIFICATION_ID = 4244
    const val NUDGE_NOTIFICATION_ID = 4245
        const val ACTION_START = "com.habittracker.timing.action.START"
        const val ACTION_PAUSE = "com.habittracker.timing.action.PAUSE"
        const val ACTION_RESUME = "com.habittracker.timing.action.RESUME"
    const val ACTION_RESUME_SESSION = "com.habittracker.timing.action.RESUME_SESSION"
        const val ACTION_COMPLETE = "com.habittracker.timing.action.COMPLETE"
        const val ACTION_STOP = "com.habittracker.timing.action.STOP"
        const val ACTION_EXTEND_5M = "com.habittracker.timing.action.EXTEND_5M"
    const val ACTION_ADD_1M = "com.habittracker.timing.action.ADD_1M"
    const val ACTION_SUB_1M = "com.habittracker.timing.action.SUB_1M"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_TIMER_TYPE = "extra_timer_type"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    const val EXTRA_SESSION_ID = "extra_session_id"
    
        // Actions that should be debounced to prevent double-tap bugs
        private val DEBOUNCED_ACTIONS = setOf(
            ACTION_START,
            ACTION_PAUSE,
            ACTION_RESUME,
            ACTION_COMPLETE,
            ACTION_STOP
        )
        private const val DEFAULT_MINUTES = 25L
    // Default focus profile thresholds (percent of duration)
    private val DEFAULT_FOCUS_THRESHOLDS = listOf(0,25,50,75,100)
    private const val DEFAULT_PROFILE_ID = "focus"
    }

    private fun postSystemFinalAlertNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_FINAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Timer Complete")
            .setContentText("Session finished")
            .setAutoCancel(true)
            .build()
        nm.notify(FINAL_ALERT_NOTIFICATION_ID, notification)
    }

    private fun cancelHeadsUpIfAny() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(HEADS_UP_NOTIFICATION_ID)
    }

    private fun postHeadsUpFinalNotification(remainingMs: Long) {
        val totalSeconds = (remainingMs / 1000)
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        
        val text = if (hours > 0) {
            String.format("%d:%02d:%02d remaining — last seconds", hours, mins, secs)
        } else {
            String.format("%d:%02d remaining — last seconds", mins, secs)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID_HEADS_UP)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(mainContentPendingIntent())
            .addAction(0, getString(R.string.action_extend_5m), servicePendingIntent(ACTION_EXTEND_5M))
            .addAction(0, getString(R.string.action_complete), servicePendingIntent(ACTION_COMPLETE))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.priority = NotificationCompat.PRIORITY_HIGH
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Auto-timeout after ~10s window
            builder.setTimeoutAfter(11_000)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(HEADS_UP_NOTIFICATION_ID, builder.build())
    }

    private fun postGentleNudgeNotification(remainingMs: Long) {
        // Round up to nearest minute for friendlier copy; ensure at least 1 minute
        val minutesLeft = (((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1)).toInt()
        val text = getString(R.string.nudge_near_target, minutesLeft)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainContentPendingIntent())
            .addAction(0, getString(R.string.action_extend_5m), servicePendingIntent(ACTION_EXTEND_5M))
            .addAction(0, getString(R.string.action_add_1m), servicePendingIntent(ACTION_ADD_1M))
            .addAction(0, getString(R.string.action_complete), servicePendingIntent(ACTION_COMPLETE))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.priority = NotificationCompat.PRIORITY_LOW
        }
        // Auto-timeout after ~2 minutes (covers most pre-target windows)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(120_000)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NUDGE_NOTIFICATION_ID, builder.build())
    }
}

// Gating / decision extraction (testable)
data class AlertDecision(
    val playAny: Boolean,
    val playSound: Boolean,
    val playHaptics: Boolean,
    val spokenText: String?,
    val rawResId: Int?,
    val updateLastNonFinal: Boolean,
    val systemFinalNotification: Boolean
)

object TimerAlertGating {
    fun decide(
        prefs: com.habittracker.data.preferences.TimingPreferences,
        eventType: AlertType,
        lastNonFinalAt: Long,
        now: Long,
        soundPack: TimerSoundPack
    ): AlertDecision {
        val spoken = when (eventType) {
            AlertType.FINAL -> appContext().getString(R.string.tts_timer_complete)
            AlertType.MIDPOINT -> appContext().getString(R.string.tts_timer_halfway)
            AlertType.START -> appContext().getString(R.string.tts_timer_started)
            else -> null
        }
        val isFinal = eventType == AlertType.FINAL
        val soundBase = prefs.enableGlobalAudioCues
        val haptics = prefs.enableHaptics && !prefs.reducedMotion
        val tts = prefs.enableTts
        val isProgressLike = eventType == AlertType.PROGRESS || eventType == AlertType.MIDPOINT
        val progressAllowed = prefs.enableProgressCues && isProgressLike
        val throttled = !isFinal && (now - lastNonFinalAt < 10_000)
        val wantsSound = soundBase && !throttled && (isFinal || progressAllowed || eventType == AlertType.START)
        val rawRes = if (wantsSound) soundPack.resIdFor(eventType) else null
        val useSystemFinal = isFinal && soundPack.id == "system"
        val effectivePlaySound = wantsSound && rawRes != null && !useSystemFinal
        val playAny = effectivePlaySound || haptics || (tts && spoken != null) || useSystemFinal
        val updateLast = !isFinal && effectivePlaySound
        val spokenOut = if (tts) spoken else null
        return AlertDecision(
            playAny = playAny,
            playSound = effectivePlaySound,
            playHaptics = haptics,
            spokenText = spokenOut,
            rawResId = rawRes,
            updateLastNonFinal = updateLast,
            systemFinalNotification = useSystemFinal
        )
    }
}

private fun appContext(): Context {
    // Use reflection fallback to application context if needed; simplified using TimerService::class.java is not ideal.
    return try {
        val clazz = Class.forName("android.app.ActivityThread")
        val method = clazz.getMethod("currentApplication")
        (method.invoke(null) as? Context) ?: throw IllegalStateException("No app context")
    } catch (e: Exception) {
        throw IllegalStateException("Application context unavailable for TTS localization", e)
    }
}

// Lightweight event bus for ticks
object TimerBus {
    private val _events = MutableSharedFlow<TimerEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()
    fun emit(event: TimerEvent) { _events.tryEmit(event) }
}

sealed class TimerEvent {
    data class Started(val sessionId: Long, val habitId: Long, val targetMs: Long, val autoComplete: Boolean) : TimerEvent()
    data class Tick(val sessionId: Long, val habitId: Long, val remainingMs: Long) : TimerEvent()
    data class Paused(val sessionId: Long, val habitId: Long) : TimerEvent()
    data class Resumed(val sessionId: Long, val habitId: Long) : TimerEvent()
    data class Completed(val sessionId: Long, val habitId: Long) : TimerEvent()
    data class Extended(val sessionId: Long, val habitId: Long, val addedMs: Long, val newTargetMs: Long) : TimerEvent()
    data class Error(val habitId: Long, val message: String) : TimerEvent()
    // Phase UIX-11
    data class Overtime(val sessionId: Long, val habitId: Long, val overtimeMs: Long) : TimerEvent()
    // Phase 2
    data class NearTarget(val sessionId: Long, val habitId: Long, val remainingMs: Long) : TimerEvent()
    // Phase 2: Announce when a previously running session was auto-paused due to single-active enforcement
    data class AutoPaused(val pausedSessionId: Long, val pausedHabitId: Long) : TimerEvent()
    // Phase 5: Pomodoro segment changes
    data class SegmentChanged(val sessionId: Long, val habitId: Long, val inBreak: Boolean, val cycleCount: Int) : TimerEvent()
}

// Phase UIX-2: Alert scheduling types & bus
enum class AlertType { START, PROGRESS, MIDPOINT, FINAL }

data class TimerAlertEvent(
    val habitId: Long,
    val sessionId: Long,
    val percent: Int,
    val remainingMs: Long,
    val type: AlertType,
    val emittedAt: Long
)

data class AlertPoint(val percent: Int, val triggerElapsedMs: Long, val type: AlertType)

object AlertScheduleBuilder {
    fun build(targetDurationMs: Long, percentages: List<Int>): List<AlertPoint> {
        val sanitized = percentages.filter { it in 0..100 }.distinct().sorted()
        return sanitized.map { pct ->
            val type = when (pct) {
                0 -> AlertType.START
                50 -> AlertType.MIDPOINT
                100 -> AlertType.FINAL
                else -> AlertType.PROGRESS
            }
            val trigger = ((pct / 100.0) * targetDurationMs).toLong()
            AlertPoint(pct, trigger, type)
        }
    }
}

object TimerAlertBus {
    private val _events = MutableSharedFlow<TimerAlertEvent>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()
    fun emit(event: TimerAlertEvent) { _events.tryEmit(event) }
}


