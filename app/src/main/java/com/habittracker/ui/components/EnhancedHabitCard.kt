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
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerCompletionInteractor.ConfirmType
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.resolveTimerUxEntryPoint
import com.habittracker.timing.TimerFeatureFlags
import com.habittracker.ui.modifiers.disableDuringTimerAction
import com.habittracker.ui.utils.TimerActionEventEffect

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
    onOvertimeExtend: (HabitUiModel) -> Unit = {},
    onOvertimeComplete: (HabitUiModel) -> Unit = {},
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
    val useCoordinator = TimerFeatureFlags.enableActionCoordinator
    val handler = remember(context, useCoordinator) {
        if (useCoordinator) resolveTimerUxEntryPoint(context).timerActionHandler() else null
    }
    val coordinatorState by if (handler != null) {
        handler.state.collectAsState()
    } else {
        remember(handler) { mutableStateOf(TimerActionCoordinator.CoordinatorState()) }
    }
    val waitingOnThisHabit = handler != null && coordinatorState.trackedHabitId == habit.id && coordinatorState.waitingForService
    val controlsEnabled = handler == null || !waitingOnThisHabit
    val controlModifier = if (handler != null && coordinatorState.trackedHabitId == habit.id) {
        Modifier.disableDuringTimerAction(coordinatorState)
    } else {
        Modifier
    }

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
    val pausedForUi = if (handler != null && coordinatorState.trackedHabitId == habit.id) {
        coordinatorState.paused
    } else {
        isPaused
    }
    
    // Timing feature state
    val userEngagementLevel by timingViewModel.userEngagementLevel.collectAsState()
    // val availableFeatures by timingViewModel.availableFeatures.collectAsState() // currently unused
    
    // Date formatter for future use
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    
    // Phase 1: Interactor wiring state
    val interactor = remember(handler) {
        if (handler == null) com.habittracker.timerux.TimerCompletionInteractor() else null
    }
    var confirmBelowMin by remember { mutableStateOf<Int?>(null) }
    var confirmDiscardElapsedSec by remember { mutableStateOf<Int?>(null) }
    // Phase 5: End Pomodoro Early confirmation
    // Phase 5: End Pomodoro Early confirmation
    var confirmEndPomodoroEarly by remember { mutableStateOf<Boolean>(false) }
    // New: Complete without timer confirmation
    var confirmCompleteWithoutTimer by remember { mutableStateOf(false) }

    if (handler != null) {
        TimerActionEventEffect(
            handler = handler,
            onConfirm = { event ->
                if (event.habitId != habit.id) return@TimerActionEventEffect
                when (event.type) {
                    ConfirmType.BelowMinDuration -> {
                        confirmBelowMin = (event.payload as? Int) ?: habit.timing?.minDuration?.seconds?.toInt()
                    }
                    ConfirmType.DiscardNonZeroSession -> {
                        confirmDiscardElapsedSec = (event.payload as? Int) ?: 0
                    }
                    ConfirmType.EndPomodoroEarly -> {
                        confirmEndPomodoroEarly = true
                    }
                                        ConfirmType.CompleteWithoutTimer -> {
                            confirmCompleteWithoutTimer = true
                        }
                    }
                },
                onSnackbar = { message -> showUndo(message) {} },
                onUndo = { message -> showUndo(message) { onUndoComplete() } },
                onTip = { message -> showUndo(message) {} },
                onCompleted = { event ->
                    if (event.habitId == habit.id) {
                        onMarkComplete()
                    }
                }
            )
    }

    Card(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            // Action buttons row (kept lightweight to avoid squeezing the title)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Checkmark button
                IconButton(
                    onClick = {
                        // Route through coordinator if timer is enabled to show confirmation dialog
                        if (handler != null && habit.timing?.timerEnabled == true) {
                            // Let coordinator decide - will show "Complete without timer?" dialog
                            handler.handle(
                                TimerIntent.Done,
                                habit.id
                            )
                        } else {
                            // No timer or coordinator disabled - direct completion
                            onMarkComplete()
                        }
                    },
                    enabled = controlsEnabled,
                    modifier = controlModifier
                        .then(Modifier.size(48.dp))
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (habit.description.isNotBlank()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Edit button
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit habit",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (!isCompact) {
                // Error display
                val runtimeError = (coordinatorState as? TimerActionCoordinator.CoordinatorState)?.error
                if (runtimeError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Error",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = runtimeError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleTimerButton(
                        habit = habit,
                        onStartTimer = { h, d ->
                            if (!controlsEnabled) return@SimpleTimerButton
                            onStartTimer(h, d)
                        },
                        modifier = controlModifier
                    )
                    // Runtime controls only when a timer is active or paused for this habit
                    if (isActive || isPaused) {
                        IconButton(
                            onClick = {
                                if (handler != null) {
                                    val intent = if (pausedForUi) TimerIntent.Resume else TimerIntent.Pause
                                    handler.handle(intent, habit.id)
                                } else {
                                    if (isPaused) timerController.resume() else timerController.pause()
                                }
                            },
                            enabled = controlsEnabled,
                            modifier = controlModifier.then(Modifier.size(48.dp))
                        ) {
                            Icon(
                                imageVector = if (pausedForUi) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (pausedForUi) "Resume timer" else "Pause timer",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (handler != null) {
                                    handler.handle(TimerIntent.Done, habit.id)
                                } else {
                                    timerController.complete()
                                }
                            },
                            enabled = controlsEnabled,
                            modifier = controlModifier.then(Modifier.size(48.dp))
                        ) {
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
                                if (handler != null) {
                                    handler.handle(
                                        TimerIntent.Done,
                                        habit.id,
                                        TimerActionCoordinator.DecisionContext(
                                            confirmation = TimerActionCoordinator.ConfirmationOverride.END_POMODORO_EARLY
                                        )
                                    )
                                } else {
                                    timerController.complete()
                                    onMarkComplete()
                                }
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
                                if (handler != null) {
                                    handler.handle(
                                        TimerIntent.Done,
                                        habit.id,
                                        TimerActionCoordinator.DecisionContext(
                                            confirmation = TimerActionCoordinator.ConfirmationOverride.COMPLETE_BELOW_MINIMUM
                                        )
                                    )
                                } else {
                                    onMarkComplete()
                                    showUndo("Marked as done. Undo") { onUndoComplete() }
                                }
                            }) { Text("Complete anyway") }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { confirmBelowMin = null }) { Text("Keep timing") }
                                TextButton(onClick = {
                                    confirmBelowMin = null
                                    if (handler != null) {
                                        handler.handle(
                                            TimerIntent.StopWithoutComplete,
                                            habit.id,
                                            TimerActionCoordinator.DecisionContext(
                                                confirmation = TimerActionCoordinator.ConfirmationOverride.LOG_PARTIAL_BELOW_MINIMUM
                                            )
                                        )
                                    } else {
                                        timerController.stop()
                                        showUndo("Logged as partial. Undo") { onUndoComplete() }
                                    }
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
                                if (handler != null) {
                                    handler.handle(
                                        TimerIntent.StopWithoutComplete,
                                        habit.id,
                                        TimerActionCoordinator.DecisionContext(
                                            confirmation = TimerActionCoordinator.ConfirmationOverride.DISCARD_SESSION
                                        )
                                    )
                                } else {
                                    timerController.stop()
                                    showUndo("Session discarded") {}
                                }
                            }) { Text("Discard") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDiscardElapsedSec = null }) { Text("Cancel") }
                        }
                    )

                }

                // Complete without timer confirmation dialog
                if (confirmCompleteWithoutTimer) {
                    var dontAskAgain by remember { mutableStateOf(false) }
                    
                    // Signal to coordinator that dialog is open (prevents conflicting actions)
                    LaunchedEffect(Unit) {
                        handler?.setPendingConfirmation(habit.id, ConfirmType.CompleteWithoutTimer)
                    }
                    
                    AlertDialog(
                        onDismissRequest = {
                            confirmCompleteWithoutTimer = false
                            handler?.clearPendingConfirmation()
                        },
                        title = { Text("Complete without tracking time?") },
                        text = {
                            Column {
                                Text(
                                    "You have a timer enabled for this habit. Would you like to:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Option explanations
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Complete without timing",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Timer,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Start timer for this session",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { dontAskAgain = !dontAskAgain }
                                ) {
                                    Checkbox(
                                        checked = dontAskAgain,
                                        onCheckedChange = { dontAskAgain = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Don't ask me again", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        },
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // PRIMARY: Complete Anyway (per user preference)
                                Button(
                                    onClick = {
                                        confirmCompleteWithoutTimer = false
                                        handler?.clearPendingConfirmation()
                                        if (dontAskAgain) {
                                            timingViewModel.setAskToCompleteWithoutTimer(false)
                                        }
                                        if (handler != null) {
                                            handler.handle(
                                                TimerIntent.Done,
                                                habit.id,
                                                TimerActionCoordinator.DecisionContext(
                                                    confirmation = TimerActionCoordinator.ConfirmationOverride.COMPLETE_WITHOUT_TIMER
                                                )
                                            )
                                        } else {
                                            onMarkComplete()
                                        }
                                    }
                                ) {
                                    Text("Complete Anyway")
                                }
                                
                                // SECONDARY: Start Timer
                                TextButton(
                                    onClick = {
                                        confirmCompleteWithoutTimer = false
                                        handler?.clearPendingConfirmation()
                                        // Start timer through coordinator
                                        if (handler != null) {
                                            handler.handle(
                                                TimerIntent.Start,
                                                habit.id
                                            )
                                        } else {
                                            // Fallback: use callback
                                            val duration = habit.timing?.estimatedDuration ?: Duration.ofMinutes(25)
                                            onStartTimer(habit, duration)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start Timer")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                confirmCompleteWithoutTimer = false
                                handler?.clearPendingConfirmation()
                            }) {
                                Text("Cancel")
                            }
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
                                onOvertimeExtend(habit)
                                timerController.extendFiveMinutes()
                            }) {
                                Text("+5m")
                            }
                            TextButton(onClick = {
                                onOvertimeComplete(habit)
                                if (handler != null) {
                                    handler.handle(TimerIntent.Done, habit.id)
                                } else {
                                    timerController.complete()
                                }
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
                                if (!timingViewModel.isFeatureEnabled(Feature.SIMPLE_TIMER)) {
                                    timingViewModel.enableFeature(Feature.SIMPLE_TIMER)
                                }
                                val dur = it.suggestedDuration
                                    ?: habit.estimatedDuration
                                    ?: java.time.Duration.ofMinutes(25)
                                onApplySuggestion(it)
                                if (handler != null) {
                                    handler.handle(
                                        TimerIntent.Start,
                                        habit.id,
                                        TimerActionCoordinator.DecisionContext(smartDuration = dur)
                                    )
                                } else {
                                    timerController.start(habit.id, TimerType.SIMPLE, dur)
                                }
                                timingViewModel.recordSuggestionInteraction(it, accepted = true)
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


