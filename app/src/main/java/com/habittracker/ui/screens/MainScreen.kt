package com.habittracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.habittracker.analytics.presentation.viewmodel.AnalyticsViewModel
import com.habittracker.ui.models.HabitUiModel
import com.habittracker.onboarding.components.rememberTooltipTarget
import com.habittracker.onboarding.manager.TooltipDisplay
import com.habittracker.onboarding.manager.TooltipManager
import com.habittracker.onboarding.manager.rememberTooltipManager
import com.habittracker.onboarding.OnboardingPreferences
import com.habittracker.ui.navigation.NavigationViewModel
import com.habittracker.presentation.viewmodel.HabitViewModel
import com.habittracker.ui.components.EmptyStateComponent
import com.habittracker.ui.components.EnhancedHabitCard
import com.habittracker.ui.components.timer.MiniSessionBar
import com.habittracker.ui.components.LoadingComponent
import com.habittracker.nudges.viewmodel.NudgeViewModel
import com.habittracker.nudges.ui.NudgeBannerSection
import com.habittracker.ui.viewmodels.timing.TimingFeatureViewModel
import com.habittracker.ui.components.timer.TimerSwitcherSheet
import com.habittracker.ui.components.timer.TimerSwitcherSession
import com.habittracker.ui.components.timer.rememberTimerSwitcherState
import com.habittracker.timing.TimerBus
import com.habittracker.timing.TimerEvent
import com.habittracker.timing.TimerController
import com.habittracker.ui.viewmodels.timing.TimerTickerViewModel
import androidx.compose.ui.platform.LocalContext
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerCompletionInteractor.ConfirmType
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.resolveTimerUxEntryPoint
import com.habittracker.timing.TimerFeatureFlags
import com.habittracker.ui.utils.TimerActionTelemetryEffect
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val LOCAL_DEBUG_ONBOARDING = true

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: HabitViewModel,
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (Long) -> Unit,
    onNavigateToHabitDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSmartTimingSettings: () -> Unit = {},
    onNavigateToReminderSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToExport: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    val hydrated by viewModel.hydratedHabits.collectAsStateWithLifecycle()
    
    // Nudges integration
    val nudgeViewModel: NudgeViewModel = hiltViewModel()
    
    // Analytics integration
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()

    val context = LocalContext.current
    val useActionCoordinator = TimerFeatureFlags.enableActionCoordinator
    val timerActionHandler = remember(context, useActionCoordinator) {
        if (useActionCoordinator) resolveTimerUxEntryPoint(context).timerActionHandler() else null
    }

    TimerActionTelemetryEffect(handler = timerActionHandler) { telemetry ->
        val analyticsEvent = telemetry.toAnalyticsEvent() ?: return@TimerActionTelemetryEffect
        analyticsViewModel.trackTimerEvent(
            eventType = analyticsEvent.type,
            habitId = telemetry.habitId,
            extra = analyticsEvent.extra
        )
    }

    // Track screen visit
    LaunchedEffect(Unit) {
        analyticsViewModel.trackScreenVisit("MainScreen")
    }
    
    // Tooltip integration
    val tooltipManager: TooltipManager = rememberTooltipManager()
    
    // Timing feature progressive disclosure
    val timingFeatureViewModel: TimingFeatureViewModel = hiltViewModel()
    
    var isGridView by remember { mutableStateOf(false) }
    var showCompletedOnly by remember { mutableStateOf(false) }
    
    // Start guided tour once after first habit layout (anchors) if onboarding done
    var guidedTourStarted by remember { mutableStateOf(false) }
    val onboardingPreferences: OnboardingPreferences = hiltViewModel<NavigationViewModel>().onboardingPreferences
    LaunchedEffect(habits.isNotEmpty()) {
        if (!guidedTourStarted && habits.isNotEmpty() && onboardingPreferences.isOnboardingCompleted()) {
            withFrameNanos { } // ensure anchors recorded
            tooltipManager.startGuidedTour()
            guidedTourStarted = true
        }
    }
    
    // Feature discovery tooltips - show when user levels up and unlocks new timing features
    LaunchedEffect(Unit) {
        timingFeatureViewModel.featureDiscoveryTrigger.collect { tooltipId ->
            // Small delay to let UI settle after level-up animation
            kotlinx.coroutines.delay(800)
            tooltipManager.showFeatureDiscoveryTooltip(tooltipId)
        }
    }
    
    // Filter habits based on toggle and convert to UI models
    val filteredHabits = remember(hydrated, showCompletedOnly) {
        val filtered = if (showCompletedOnly) {
            hydrated.filter { it.lastCompletedDate == LocalDate.now() }
        } else {
            hydrated
        }
        filtered
    }
    
    // Handle messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    
    // Timer switcher for improved single-active-timer UX
    val timerSwitcherState = rememberTimerSwitcherState()
    val tickerViewModel: TimerTickerViewModel = hiltViewModel()
    val timerController = remember(context) { TimerController(context) }
    val remainingByHabit by tickerViewModel.remainingByHabit.collectAsState()
    val pausedByHabit by tickerViewModel.pausedByHabit.collectAsState()
    
    // Listen for AutoPaused events and show timer switcher sheet instead of snackbar
    LaunchedEffect(Unit) {
        TimerBus.events.collect { evt ->
            if (evt is TimerEvent.AutoPaused) {
                // Find the habit names from hydrated list
                val pausedHabit = hydrated.find { it.id == evt.pausedHabitId }
                val activeHabitId = hydrated.find { habit ->
                    remainingByHabit[habit.id]?.let { it > 0 } == true && 
                    pausedByHabit[habit.id] != true &&
                    habit.id != evt.pausedHabitId
                }?.id
                
                val activeHabit = activeHabitId?.let { id -> hydrated.find { it.id == id } }
                
                if (pausedHabit != null && activeHabit != null) {
                    timerSwitcherState.show(
                        active = TimerSwitcherSession(
                            sessionId = 0, // Session ID not needed for display
                            habitId = activeHabit.id,
                            habitName = activeHabit.name,
                            remainingMs = remainingByHabit[activeHabit.id] ?: 0L,
                            isPaused = false,
                            isActive = true
                        ),
                        paused = TimerSwitcherSession(
                            sessionId = evt.pausedSessionId,
                            habitId = evt.pausedHabitId,
                            habitName = pausedHabit.name,
                            remainingMs = remainingByHabit[evt.pausedHabitId] ?: 0L,
                            isPaused = true,
                            isActive = false
                        )
                    )
                } else {
                    // Fallback to snackbar if we can't identify the habits
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Paused previous timer",
                            actionLabel = "Resume",
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        ).let { result ->
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                timerController.resumeSession(evt.pausedSessionId)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Timer switcher bottom sheet
    if (timerSwitcherState.isVisible && timerSwitcherState.activeSession != null && timerSwitcherState.pausedSession != null) {
        TimerSwitcherSheet(
            activeSession = timerSwitcherState.activeSession!!,
            pausedSession = timerSwitcherState.pausedSession!!,
            onSwitchToActive = {
                timerSwitcherState.dismiss()
            },
            onSwitchToPaused = {
                // Pause current, resume paused
                timerController.pause()
                timerSwitcherState.pausedSession?.sessionId?.let { sessionId ->
                    timerController.resumeSession(sessionId)
                }
                timerSwitcherState.dismiss()
            },
            onDismiss = {
                timerSwitcherState.dismiss()
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Habits",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Today • ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Modern action menu - consolidate actions into overflow menu
                    var showMenu by remember { mutableStateOf(false) }
                    
                    // Primary actions (most used) - only 2-3 icons max
                    IconButton(
                        onClick = { showCompletedOnly = !showCompletedOnly }
                    ) {
                        Icon(
                            imageVector = if (showCompletedOnly) Icons.Filled.FilterList else Icons.Filled.FilterListOff,
                            contentDescription = if (showCompletedOnly) "Show all habits" else "Show completed only",
                            tint = if (showCompletedOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = rememberTooltipTarget("view_toggle")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = if (isGridView) "List view" else "Grid view"
                        )
                    }
                    
                    // Overflow menu for secondary actions
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = rememberTooltipTarget("menu_overflow")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Debug-only reset option to fully reset onboarding & tooltips
                            if (LOCAL_DEBUG_ONBOARDING) {
                                DropdownMenuItem(
                                    text = { Text("Reset Onboarding (Debug)") },
                                    onClick = {
                                        showMenu = false
                                        onboardingPreferences.resetAll()
                                        // Provide immediate user feedback via console log
                                        println("Debug: Onboarding & tooltips reset")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Restore, contentDescription = null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Smart Timing") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSmartTimingSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Timer, contentDescription = null)
                                },
                                modifier = rememberTooltipTarget("smart_timing_menu")
                            )

                            DropdownMenuItem(
                                text = { Text("Reminders") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToReminderSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                                },
                                modifier = rememberTooltipTarget("reminder_settings_menu")
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Analytics") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAnalytics()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Analytics, contentDescription = null)
                                },
                                modifier = rememberTooltipTarget("analytics_menu")
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Export Data") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToExport()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Download, contentDescription = null)
                                },
                                modifier = rememberTooltipTarget("export_menu")
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Settings, contentDescription = null)
                                },
                                modifier = rememberTooltipTarget("settings_menu")
                            )
                            
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAbout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Info, contentDescription = null)
                                },
                                modifier = rememberTooltipTarget("about_menu")
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddHabit,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Habit") },
                modifier = rememberTooltipTarget("add_habit_fab")
            )
    },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 88.dp // Extra space for FAB with proper breathing room
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing for better hierarchy
            ) {
                // Progress indicator
                if (uiState.isLoading) {
                    item {
                        LoadingComponent()
                    }
                }
                
                // Messages with improved styling
                uiState.errorMessage?.let { message ->
                    item {
                        MessageCard(
                            message = message,
                            isError = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                uiState.successMessage?.let { message ->
                    item {
                        MessageCard(
                            message = message,
                            isError = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Nudge banners section with better spacing
                item {
                    NudgeBannerSection(
                        viewModel = nudgeViewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Removed temporary test nudge banner to avoid interference
                
                // Statistics Card with modern design
                if (hydrated.isNotEmpty()) {
                    item {
                        StatisticsCard(
                            habits = hydrated,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Content
                if (filteredHabits.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyStateComponent(
                            title = if (showCompletedOnly) "No completed habits today" else "No habits yet",
                            description = if (showCompletedOnly) "Complete some habits to see them here!" else "Start building better habits today",
                            actionText = if (!showCompletedOnly) "Add Your First Habit" else null,
                            onAction = if (!showCompletedOnly) onNavigateToAddHabit else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp) // Optimized height for better proportions
                        )
                    }
                } else {
                    // Habit items with improved responsive grid
                    if (isGridView) {
                        // Modern responsive grid layout
                        val chunkedHabits = filteredHabits.chunked(2)
                        items(chunkedHabits) { habitPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp) // Better spacing
                            ) {
                                habitPair.forEach { habit ->
                                    EnhancedHabitCard(
                                        habit = habit,
                                        onMarkComplete = {
                                            if (timerActionHandler != null) {
                                                // Use coordinator for proper confirmation flow
                                                // Intent.Done checks for timer state and shows confirmation if needed
                                                timerActionHandler.handle(TimerIntent.Done, habit.id)
                                            } else {
                                                // Fallback to direct completion
                                                viewModel.markHabitComplete(habit.id)
                                            }
                                        },
                                        onUndoComplete = { viewModel.unmarkHabitForToday(habit.id) },
                                        showUndo = { message, onUndo ->
                                            snackbarScope.launch {
                                                val res = snackbarHostState.showSnackbar(
                                                    message = message,
                                                    actionLabel = "Undo"
                                                )
                                                if (res == SnackbarResult.ActionPerformed) onUndo()
                                            }
                                        },
                                        onClick = {
                                            // Only navigate when there is at least one completion
                                            if (habit.lastCompletedDate != null) {
                                                onNavigateToHabitDetail(habit.id)
                                            } else {
                                                snackbarScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "No analytics for this habit yet — complete it at least once."
                                                    )
                                                }
                                            }
                                        },
                                        onEditClick = { onNavigateToEditHabit(habit.id) },
                                        onOvertimeExtend = {
                                            analyticsViewModel.trackScreenVisit("OvertimeExtend", fromScreen = "EnhancedHabitCard")
                                        },
                                        onOvertimeComplete = {
                                            analyticsViewModel.trackHabitCompletion(
                                                habitId = it.id.toString(),
                                                habitName = it.name,
                                                isCompleted = true
                                            )
                                        },
                                        isCompact = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill remaining space if odd number of items
                                if (habitPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // Enhanced list view with proper spacing
                        items(
                            items = filteredHabits,
                            key = { it.id }
                        ) { habit ->
                            EnhancedHabitCard(
                                habit = habit,
                                onMarkComplete = {
                                    if (timerActionHandler != null) {
                                        // Use coordinator for proper confirmation flow
                                        // Intent.Done checks for timer state and shows confirmation if needed
                                        timerActionHandler.handle(TimerIntent.Done, habit.id)
                                    } else {
                                        // Fallback to direct completion
                                        viewModel.markHabitComplete(habit.id)
                                    }
                                },
                                onUndoComplete = { viewModel.unmarkHabitForToday(habit.id) },
                                showUndo = { message, onUndo ->
                                    snackbarScope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = message,
                                            actionLabel = "Undo"
                                        )
                                        if (res == SnackbarResult.ActionPerformed) onUndo()
                                    }
                                },
                                onClick = {
                                    // Only navigate when there is at least one completion
                                    if (habit.lastCompletedDate != null) {
                                        onNavigateToHabitDetail(habit.id)
                                    } else {
                                        snackbarScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "No analytics for this habit yet — complete it at least once."
                                            )
                                        }
                                    }
                                },
                                onEditClick = { onNavigateToEditHabit(habit.id) },
                                onOvertimeExtend = {
                                    analyticsViewModel.trackScreenVisit("OvertimeExtend", fromScreen = "EnhancedHabitCard")
                                },
                                onOvertimeComplete = {
                                    analyticsViewModel.trackHabitCompletion(
                                        habitId = it.id.toString(),
                                        habitName = it.name,
                                        isCompleted = true
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                            )
                        }
                    }
                }
            }
            
            // Removed intrusive NudgeOverlay to prevent blocking UI
            
            // Tooltip display for guided tour
            TooltipDisplay(tooltipManager = tooltipManager)
            // Mini session bar overlay (Phase UIX-5)
            Box(modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)) {
                MiniSessionBar()
            }
        }
    }
}

@Composable
private fun StatisticsCard(
    habits: List<HabitUiModel>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val completedToday = habits.count { 
        it.lastCompletedDate == today
    }
    val totalStreak = habits.sumOf { it.streakCount }
    val avgStreak = if (habits.isNotEmpty()) totalStreak / habits.size else 0
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = completedToday.toString(),
                label = "Today",
                icon = Icons.Filled.CheckCircle
            )
            StatItem(
                value = habits.size.toString(),
                label = "Total",
                icon = Icons.Filled.List
            )
            StatItem(
                value = avgStreak.toString(),
                label = "Avg Streak",
                icon = Icons.Filled.TrendingUp
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

// TEMPORARY: Test component to demonstrate streak warning visibility while scrolling
@Composable
private fun TestStreakWarningBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "⚠️ Streak at Risk!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Your 7-day reading streak is at risk. Complete today to keep it going!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Button(
                onClick = { /* Demo action */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Complete", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

// Extension function to convert java.util.Date to LocalDate
private fun java.util.Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
}


private data class TimerAnalyticsEvent(
    val type: String,
    val extra: Map<String, Any?> = emptyMap()
)

private fun TimerActionCoordinator.TimerActionTelemetry.toAnalyticsEvent(): TimerAnalyticsEvent? {
    return when (this) {
        is TimerActionCoordinator.TimerActionTelemetry.Executed -> {
            val eventType = intent.toAnalyticsEventType() ?: return null
            TimerAnalyticsEvent(
                type = eventType,
                extra = mapOf("intent" to intent.toAnalyticsIntentName())
            )
        }
        is TimerActionCoordinator.TimerActionTelemetry.Confirmed -> TimerAnalyticsEvent(
            type = "timer_confirm_required",
            extra = mapOf(
                "intent" to intent.toAnalyticsIntentName(),
                "confirm" to confirmType.toAnalyticsValue()
            )
        )
        is TimerActionCoordinator.TimerActionTelemetry.Disallowed -> TimerAnalyticsEvent(
            type = "timer_action_blocked",
            extra = mapOf(
                "intent" to intent.toAnalyticsIntentName(),
                "reason" to reason
            )
        )
    }
}

private fun TimerIntent.toAnalyticsEventType(): String? = when (this) {
    TimerIntent.Start -> "timer_start"
    TimerIntent.Pause -> "timer_pause"
    TimerIntent.Resume -> "timer_resume"
    TimerIntent.Done -> "timer_done"
    TimerIntent.StopWithoutComplete -> "timer_discard"
    TimerIntent.QuickComplete -> "timer_quick_complete"
}

private fun TimerIntent.toAnalyticsIntentName(): String = when (this) {
    TimerIntent.Start -> "start"
    TimerIntent.Pause -> "pause"
    TimerIntent.Resume -> "resume"
    TimerIntent.Done -> "done"
    TimerIntent.StopWithoutComplete -> "stop_without_complete"
    TimerIntent.QuickComplete -> "quick_complete"
}

private fun ConfirmType.toAnalyticsValue(): String = when (this) {
    ConfirmType.BelowMinDuration -> "below_min_duration"
    ConfirmType.DiscardNonZeroSession -> "discard_non_zero_session"
    ConfirmType.EndPomodoroEarly -> "end_pomodoro_early"
    ConfirmType.CompleteWithoutTimer -> "complete_without_timer"
}
