package com.habittracker.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.ui.models.HabitUiModel
import com.habittracker.ui.models.timing.*
import com.habittracker.ui.components.timing.*
import com.habittracker.ui.viewmodels.timing.TimingFeatureViewModel
import com.habittracker.ui.viewmodels.timing.ActiveTimerViewModel
import com.habittracker.ui.components.timer.RadialTimer
import com.habittracker.ui.viewmodels.timing.TimerTickerViewModel
import com.habittracker.onboarding.components.rememberTooltipTarget
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Duration
import java.util.*
import kotlinx.coroutines.launch

/**
 * Enhanced HabitCard with Phase 2 Progressive Timing Features
 * 
 * Integrates timing features based on user engagement level:
 * - Level 0: Standard card (no timing UI)
 * - Level 1: Simple timer button
 * - Level 2: + Smart suggestions
 * - Level 3: Full timing controls
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EnhancedHabitCard(
    habit: HabitUiModel,
    onMarkComplete: () -> Unit,
    // Phase 1: Provide an undo snackbar hook from parent Scaffold
    onUndoComplete: () -> Unit = {},
    showUndo: (message: String, onUndo: () -> Unit) -> Unit = { _, _ -> },
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onStartTimer: (HabitUiModel, Duration) -> Unit = { _, _ -> },
    onApplySuggestion: (SmartSuggestion) -> Unit = { },
    onDismissSuggestion: (SmartSuggestion) -> Unit = { },
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    timingViewModel: TimingFeatureViewModel = hiltViewModel()
) {
    val today = LocalDate.now()
    val isCompletedToday = habit.lastCompletedDate == today
    // Only allow navigating to details/analytics if the habit has been completed at least once
    val hasAnyCompletion = habit.lastCompletedDate != null
    val context = androidx.compose.ui.platform.LocalContext.current
    val timerController = remember(context) { com.habittracker.timing.TimerController(context) }
    // Phase 2 UX: show snackbar when another timer is auto-paused due to single-active enforcement
    LaunchedEffect(Unit) {
        com.habittracker.timing.TimerBus.events.collect { evt ->
            if (evt is com.habittracker.timing.TimerEvent.AutoPaused) {
                // Ideally include habit name; fallback to generic copy per roadmap
                showUndo("Paused previous timer — Resume") {
                    // True exact-session resume
                    timerController.resumeSession(evt.pausedSessionId)
                }
            }
        }
    }
    val tickerViewModel: TimerTickerViewModel = hiltViewModel()
    val activeTimerVm: ActiveTimerViewModel = hiltViewModel()
    val analyticsVm: com.habittracker.analytics.presentation.viewmodel.AnalyticsViewModel = hiltViewModel()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Local error banner state (collect from TimingFeatureViewModel)
    var errorBanner by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        timingViewModel.errorMessages.collect { msg ->
            errorBanner = msg
            // Auto-dismiss after a short delay to avoid persistent clutter
            kotlinx.coroutines.delay(4_000)
            if (errorBanner == msg) errorBanner = null
        }
    }
    val pausedByHabit by tickerViewModel.pausedByHabit.collectAsState()
    val errorsByHabit by tickerViewModel.errorsByHabit.collectAsState()
    val isPaused = pausedByHabit[habit.id] == true
    val remainingByHabit by tickerViewModel.remainingByHabit.collectAsState()
    val isActive = remainingByHabit[habit.id]?.let { it > 0 } == true
    
    // Timing feature state
    val userEngagementLevel by timingViewModel.userEngagementLevel.collectAsState()
    // val availableFeatures by timingViewModel.availableFeatures.collectAsState() // currently unused
    
    // Date formatter for future use
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    
    // Phase 1: Interactor wiring state
    val interactor = remember { com.habittracker.timerux.TimerCompletionInteractor() }
    var confirmBelowMin by remember { mutableStateOf<Int?>(null) }
    var confirmDiscardElapsedSec by remember { mutableStateOf<Int?>(null) }
    // Phase 5: End Pomodoro Early confirmation
    var confirmEndPomodoroEarly by remember { mutableStateOf<Boolean>(false) }

    var showControlSheet by remember { mutableStateOf(false) }
    // Optional note dialog for partials
    var showNoteDialog by remember { mutableStateOf(false) }
    var pendingPartialNote by remember { mutableStateOf("") }
    val partialVm: com.habittracker.ui.viewmodels.timing.PartialSessionViewModel = hiltViewModel()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (hasAnyCompletion) {
                    onClick()
                } else {
                    // Professional redirect instead of a crashy navigation
                    showUndo("No analytics for this habit yet — complete it at least once.") { }
                }
            }
            // Anchor for tutorial/tooltips to highlight the card
            .then(rememberTooltipTarget("habit_card"))
            .then(rememberTooltipTarget("enhanced_habit_card")),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompletedToday) 6.dp else 3.dp,
            pressedElevation = if (isCompletedToday) 8.dp else 5.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompletedToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 12.dp else 16.dp,
                vertical = if (isCompact) 12.dp else 16.dp
            )
        ) {
            // Header Row - Same as original
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habit.name,
                            style = if (isCompact) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = if (isCompact) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Timer status indicator (subtle)
                        if (habit.isTimerActive == true) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    
                    if (!isCompact && habit.description.isNotBlank()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Friendly hint for habits with no completions yet
                    if (!isCompact && !hasAnyCompletion) {
                        Row(
                            modifier = Modifier
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Complete once to unlock insights",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Smart timing info (Level 2+)
                    if (!isCompact && userEngagementLevel != UserEngagementLevel.Casual) {
                        habit.nextSuggestedTime?.let { suggestedTime ->
                            Text(
                                text = "💡 Optimal time: ${suggestedTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    // Show last completed date if available
                    if (!isCompact && habit.lastCompletedDate != null && habit.lastCompletedDate != today) {
                        val lastCompletedText = remember(habit.lastCompletedDate) {
                            "Last: ${dateFormatter.format(java.sql.Date.valueOf(habit.lastCompletedDate.toString()))}"
                        }
                        Text(
                            text = lastCompletedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Action buttons row (kept lightweight to avoid squeezing the title)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isCompact) {
                        // Edit (keep in header)
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit habit",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Mark habit complete (always visible)
                    IconButton(
                        onClick = onMarkComplete,
                        modifier = Modifier
                            .size(48.dp)
                            // Anchor for tutorial/tooltips to highlight complete action
                            .then(rememberTooltipTarget("habit_complete_button"))
                    ) {
                        Icon(
                            imageVector = if (isCompletedToday) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (isCompletedToday) "Completed" else "Mark complete",
                            tint = if (isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (!isCompact) {
                Spacer(modifier = Modifier.height(12.dp))
                // Error banner (ephemeral)
                val runtimeError = errorsByHabit[habit.id]
                AnimatedVisibility(visible = runtimeError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    // Phase 1: Route to interactor
                                    val timing = habit.timing
                                    val session = habit.timerSession
                                    val inputs = com.habittracker.timerux.TimerCompletionInteractor.Inputs(
                                        habitId = habit.id,
                                        timerEnabled = timing?.timerEnabled == true,
                                        requireTimerToComplete = timing?.requireTimerToComplete == true,
                                        minDurationSec = timing?.minDuration?.seconds?.toInt(),
                                        targetDurationSec = timing?.estimatedDuration?.seconds?.toInt(),
                                        timerState = when {
                                            session?.isRunning == true -> com.habittracker.timerux.TimerCompletionInteractor.TimerState.RUNNING
                                            session?.isPaused == true -> com.habittracker.timerux.TimerCompletionInteractor.TimerState.PAUSED
                                            else -> com.habittracker.timerux.TimerCompletionInteractor.TimerState.IDLE
                                        },
                                        elapsedSec = session?.elapsedTime?.seconds?.toInt() ?: 0,
                                        todayCompleted = habit.lastCompletedDate == java.time.LocalDate.now(),
                                        platform = com.habittracker.timerux.TimerCompletionInteractor.Platform.APP,
                                        singleActiveTimer = true,
                                        timerType = session?.type,
                                        isInBreak = session?.isInBreak == true
                                    )
                                    val outcome = interactor.decide(
                                        if (timing?.timerEnabled == true) com.habittracker.timerux.TimerCompletionInteractor.Intent.Done
                                        else com.habittracker.timerux.TimerCompletionInteractor.Intent.QuickComplete,
                                        inputs
                                    )
                                    when (outcome) {
                                        is com.habittracker.timerux.TimerCompletionInteractor.ActionOutcome.Execute -> {
                                            outcome.actions.forEach { act ->
                                                when (act) {
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.StartTimer -> {
                                                        val dur = timing?.estimatedDuration
                                                        timerController.start(habit.id, com.habittracker.ui.models.timing.TimerType.SIMPLE, dur)
                                                    }
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.PauseTimer -> timerController.pause()
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.ResumeTimer -> timerController.resume()
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.CompleteToday -> {
                                                        // For Phase 1, just mark complete and rely on service to record duration separately
                                                        onMarkComplete()
                                                    }
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.SavePartial -> {
                                                        // Log partial via repository if exposed; for now, show confirmation
                                                        showUndo("Logged as partial. Undo") { onUndoComplete() }
                                                    }
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.DiscardSession -> {
                                                        // Stop the active session
                                                        timerController.stop()
                                                    }
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.ShowUndo -> {
                                                        showUndo(act.message) { onUndoComplete() }
                                                    }
                                                    is com.habittracker.timerux.TimerCompletionInteractor.Action.ShowTip -> {
                                                        // Non-blocking hint; optional toast/snackbar via showUndo without undo action
                                                        showUndo(act.message) {}
                                                    }
                                                }
                                            }
                                        }
                                        is com.habittracker.timerux.TimerCompletionInteractor.ActionOutcome.Confirm -> {
                                            when (outcome.type) {
                                                com.habittracker.timerux.TimerCompletionInteractor.ConfirmType.BelowMinDuration -> {
                                                    confirmBelowMin = (outcome.payload as? Int) ?: timing?.minDuration?.seconds?.toInt()
                                                }
                                                com.habittracker.timerux.TimerCompletionInteractor.ConfirmType.DiscardNonZeroSession -> {
                                                    confirmDiscardElapsedSec = (outcome.payload as? Int) ?: 0
                                                }
                                                com.habittracker.timerux.TimerCompletionInteractor.ConfirmType.EndPomodoroEarly -> {
                                                    confirmEndPomodoroEarly = true
                                                }
                                            }
                                        }
                                        is com.habittracker.timerux.TimerCompletionInteractor.ActionOutcome.Disallow -> {
                                            showUndo(outcome.message) {}
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = runtimeError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // Primary timer controls row placed under the title to avoid replacing/squeezing text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleTimerButton(
                        habit = habit,
                        onStartTimer = { h, d ->
                            // Delegate to callback and start controller for side effects/analytics
                            onStartTimer(h, d)
                            timerController.start(h.id, TimerType.SIMPLE, d)
                            timingViewModel.recordTimerUsage(h.id, completed = false)
                        },
                        modifier = Modifier
                    )
                    // Runtime controls only when a timer is active or paused for this habit
                    if (isActive || isPaused) {
                        IconButton(onClick = { if (isPaused) timerController.resume() else timerController.pause() }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (isPaused) "Resume timer" else "Pause timer",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { timerController.complete() }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Complete session",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Phase 5: Simple confirm dialog for ending Pomodoro focus early
                if (confirmEndPomodoroEarly) {
                    AlertDialog(
                        onDismissRequest = { confirmEndPomodoroEarly = false },
                        title = { Text("End focus early?") },
                        text = { Text("End and complete the current Pomodoro?") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmEndPomodoroEarly = false
                                // Proceed as Done: send to service then mark complete
                                timerController.complete()
                                onMarkComplete()
                            }) { Text("End & Complete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmEndPomodoroEarly = false }) { Text("Keep Focus") }
                        }
                    )
                }
                
                // Below-min confirm dialog (Phase 1 guard-rail)
                val minSec = confirmBelowMin
                if (minSec != null) {
                    AlertDialog(
                        onDismissRequest = { confirmBelowMin = null },
                        title = { Text("Below minimum duration") },
                        text = { Text("Below your minimum ${minSec / 60}m. Complete anyway or log as partial?") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmBelowMin = null
                                onMarkComplete()
                                showUndo("Marked as done. Undo") { onUndoComplete() }
                            }) { Text("Complete anyway") }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { confirmBelowMin = null }) { Text("Keep timing") }
                                TextButton(onClick = {
                                    confirmBelowMin = null
                                    // Phase 1: treat as complete for simplicity; future: SavePartial
                                    onMarkComplete()
                                    showUndo("Logged as partial. Undo") { onUndoComplete() }
                                }) { Text("Log partial") }
                            }
                        }
                    )
                }

                // Discard non-zero session confirmation
                val discardSec = confirmDiscardElapsedSec
                if (discardSec != null) {
                    AlertDialog(
                        onDismissRequest = { confirmDiscardElapsedSec = null },
                        title = { Text("Discard session?") },
                        text = { Text("Discard ${discardSec / 60}m session?") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmDiscardElapsedSec = null
                                timerController.stop()
                                showUndo("Session discarded") {}
                            }) { Text("Discard") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDiscardElapsedSec = null }) { Text("Cancel") }
                        }
                    )
                }
                // Stats Row - Enhanced with timing info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show a small badge when details are gated
                    if (!hasAnyCompletion) {
                        AssistChip(
                            onClick = { /* no-op */ },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Unlock after 1 completion",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }

                    // Streak chip
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                "🔥 ${habit.streakCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = rememberTooltipTarget("streak_counter")
                    )
                    
                    // Frequency chip
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    
                    // Timer required indicator - prominent display
                    if (habit.timing?.requireTimerToComplete == true) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = { 
                                Icon(
                                    Icons.Filled.Timer, 
                                    contentDescription = "Timer required to complete", 
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = "Timer Required",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.primary,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    
                    // Avoid double-timer: show duration when inactive, countdown when active
                    val timersEnabled = timingViewModel.isFeatureEnabled(Feature.SIMPLE_TIMER)
                    if (!isActive && timersEnabled) {
                        val durMinutes = habit.estimatedDuration?.toMinutes() ?: 25
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "⏱️ ${durMinutes}min",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                    if (isActive) {
                        val uiState by activeTimerVm.state.collectAsState()
                        if (uiState.active && uiState.habitId == habit.id) {
                            val prefsVm: com.habittracker.ui.viewmodels.timing.TimingAudioSettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                            val prefs by prefsVm.prefs.collectAsState()
                            RadialTimer(
                                totalMillis = uiState.totalMs,
                                remainingMillis = uiState.remainingMs,
                                isPaused = uiState.paused,
                                modifier = Modifier.size(72.dp),
                                reducedMotion = prefs.reducedMotion
                            )
                            // Quick entry to full controls
                            TextButton(onClick = { showControlSheet = true }) {
                                Text("More")
                            }
                        } else {
                            Box(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                                com.habittracker.ui.components.timing.LiveRemainingTime(habitId = habit.id)
                            }
                        }
                    }
                }

                // Phase UIX-11: Gentle overtime nudge (appears once overtime >= 1m)
                val overtimeByHabit by tickerViewModel.overtimeByHabit.collectAsState()
                val overtimeMs = overtimeByHabit[habit.id] ?: 0L
                AnimatedVisibility(visible = overtimeMs >= 60_000L) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Overtime — +" + ((overtimeMs / 1000) / 60).coerceAtLeast(1).toString() + "m",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                // Track overtime extend action
                                analyticsVm.trackScreenVisit("OvertimeExtend", fromScreen = "EnhancedHabitCard")
                                timerController.extendFiveMinutes()
                            }) {
                                Text("+5m")
                            }
                            TextButton(onClick = {
                                // Track immediate completion from overtime nudge
                                analyticsVm.trackHabitCompletion(
                                    habitId = habit.id.toString(),
                                    habitName = habit.name,
                                    isCompleted = true
                                )
                                timerController.complete()
                            }) {
                                Text("Complete now")
                            }
                        }
                    }
                }
                
                // Progressive Timing Discovery UI
                // Note: We already surface the Start chip above; avoid duplicating it via discovery UI.
                ProgressiveTimingDiscovery(
                    habit = habit,
                    userEngagementLevel = userEngagementLevel,
                    onLevelUp = { /* Handle level up */ },
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // Smart Suggestions (Slice 2): show when available (don't hide behind a flag)
                if (habit.smartSuggestions.isNotEmpty()) {
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    habit.smartSuggestions.take(1).forEach { suggestion ->
                        SmartSuggestionCard(
                            suggestion = suggestion,
                            onApplySuggestion = { 
                                // One-tap: ensure timer feature is on, then start with suggested duration or default
                                if (!timingViewModel.isFeatureEnabled(Feature.SIMPLE_TIMER)) {
                                    timingViewModel.enableFeature(Feature.SIMPLE_TIMER)
                                }
                                val dur = it.suggestedDuration
                                    ?: habit.estimatedDuration
                                    ?: java.time.Duration.ofMinutes(25)
                                timerController.start(habit.id, TimerType.SIMPLE, dur)
                                timingViewModel.recordTimerUsage(habit.id, completed = false)
                                timingViewModel.recordSuggestionInteraction(it, accepted = true)
                                onApplySuggestion(it)
                            },
                            onDismissSuggestion = { 
                                onDismissSuggestion(it)
                                timingViewModel.recordSuggestionInteraction(it, accepted = false)
                            }
                        )
                    }
                }
                
            } else {
                // Compact view stats - Enhanced
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hasAnyCompletion) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "🔥 ${habit.streakCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Timer indicator in compact view
                        if (habit.hasTimer == true && 
                            timingViewModel.isFeatureEnabled(Feature.SIMPLE_TIMER)) {
                            Text(
                                text = "⏱️",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Text(
                        text = habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (showControlSheet) {
        com.habittracker.ui.components.timer.TimerControlSheet(
            onDismiss = { showControlSheet = false },
            onLogPartial = { _ ->
                // real persistence using PartialSessionViewModel
                val uiState = activeTimerVm.state.value
                val total = uiState.totalMs
                val rem = uiState.remainingMs
                val elapsedMs = (total - rem).coerceAtLeast(0)
                val duration = java.time.Duration.ofMillis(elapsedMs)
                // Stop service session, but don't mark complete
                timerController.stop()
                // Launch save in composition-aware scope
                scope.launch {
                    val id = runCatching { partialVm.logPartial(habit.id, duration, if (pendingPartialNote.isBlank()) null else pendingPartialNote) }.getOrNull()
                    showUndo("Logged partial ${(elapsedMs/60000).coerceAtLeast(0)}m. Undo") {
                        // No direct delete API yet; future: implement undo using DAO deleteById(id)
                        onUndoComplete()
                    }
                    pendingPartialNote = ""
                }
            },
            onAddNote = { _ ->
                pendingPartialNote = ""
                showNoteDialog = true
            }
        )
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add a note") },
            text = {
                OutlinedTextField(
                    value = pendingPartialNote,
                    onValueChange = { pendingPartialNote = it },
                    placeholder = { Text("Optional note for partial session") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPartialNote = ""; showNoteDialog = false }) { Text("Clear") }
            }
        )
    }
}
