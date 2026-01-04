package com.habittracker.ui.screens.simple

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.analytics.presentation.viewmodel.AnalyticsViewModel
import com.habittracker.domain.isCompletedThisPeriod
import com.habittracker.presentation.viewmodel.HabitViewModel
import com.habittracker.timerux.TimerActionCoordinator
import com.habittracker.timerux.TimerCompletionInteractor.Intent as TimerIntent
import com.habittracker.timerux.resolveTimerUxEntryPoint
import com.habittracker.timing.TimerFeatureFlags
import com.habittracker.ui.components.EmptyStateComponent
import com.habittracker.ui.components.simple.SimpleHabitCard
import com.habittracker.ui.components.simple.SimpleMiniSessionBar
import com.habittracker.ui.components.simple.SimpleTimerSwitcherSheet
import com.habittracker.ui.models.HabitUiModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SimpleMainScreen - Phase 2 simplified home screen.
 * 
 * Features:
 * - Single coordinatorState collection (no multiple ViewModels)
 * - App bar: "My Habits" title + "Today • Jan 3, 2026" subtitle
 * - LazyColumn for habit list
 * - Error banner when coordinatorState.lastError present
 * - SimpleMiniSessionBar for active timer
 * - SimpleTimerSwitcherSheet for timer switching
 * 
 * Target: ~300-400 lines
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMainScreen(
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Habits state
    val hydrated by viewModel.hydratedHabits.collectAsStateWithLifecycle()
    
    // Timer coordinator - single source of truth
    val timerActionHandler = remember(context) {
        if (TimerFeatureFlags.enableActionCoordinator) {
            resolveTimerUxEntryPoint(context).timerActionHandler()
        } else null
    }
    val coordinatorState by timerActionHandler?.state?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(TimerActionCoordinator.CoordinatorState()) }
    
    // Analytics tracking
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    DisposableEffect(Unit) {
        analyticsViewModel.trackScreenVisit("SimpleMainScreen")
        onDispose { analyticsViewModel.endScreenVisit() }
    }
    
    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Menu state
    var showMenu by remember { mutableStateOf(false) }
    
    // Timer switcher sheet state
    var showTimerSwitcher by remember { mutableStateOf(false) }
    
    // Show timer switcher when there's a paused habit
    LaunchedEffect(coordinatorState.pausedHabitId) {
        showTimerSwitcher = coordinatorState.pausedHabitId != null
    }
    
    // Handle coordinator UI events
    LaunchedEffect(timerActionHandler) {
        timerActionHandler?.events?.collect { event ->
            when (event) {
                is TimerActionCoordinator.UiEvent.Snackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is TimerActionCoordinator.UiEvent.Undo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo handled by coordinator
                    }
                }
                is TimerActionCoordinator.UiEvent.Completed -> {
                    // Habits auto-refresh via Flow collection
                }
                else -> { /* Other events handled elsewhere */ }
            }
        }
    }
    
    // Get active habit name for mini bar
    val activeHabit = hydrated.find { it.id == coordinatorState.trackedHabitId }
    val pausedHabit = hydrated.find { it.id == coordinatorState.pausedHabitId }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Habits",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Today • ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Analytics") },
                            onClick = { showMenu = false; onNavigateToAnalytics() }
                        )
                        DropdownMenuItem(
                            text = { Text("Timer Settings") },
                            onClick = { showMenu = false; onNavigateToSmartTimingSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text("Reminders") },
                            onClick = { showMenu = false; onNavigateToReminderSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text("Theme") },
                            onClick = { showMenu = false; onNavigateToSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Data") },
                            onClick = { showMenu = false; onNavigateToExport() }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = { showMenu = false; onNavigateToAbout() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddHabit,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Error banner
                AnimatedVisibility(
                    visible = coordinatorState.lastError != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ErrorBanner(
                        message = coordinatorState.lastError ?: "",
                        onDismiss = { timerActionHandler?.clearError() }
                    )
                }
                
                // Habit list
                if (hydrated.isEmpty()) {
                    EmptyStateComponent(
                        title = "No habits yet",
                        description = "Start building positive habits by adding your first one",
                        actionText = "Add Habit",
                        onAction = onNavigateToAddHabit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            // Extra bottom padding for mini bar + FAB
                            bottom = if (coordinatorState.trackedHabitId != null) 160.dp else 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = hydrated,
                            key = { it.id }
                        ) { habit ->
                            val isCompleted = isCompletedThisPeriod(habit.frequency, habit.lastCompletedDate)
                            val isTimerActive = coordinatorState.trackedHabitId == habit.id
                            val isPaused = isTimerActive && coordinatorState.paused
                            
                            SimpleHabitCard(
                                habit = habit,
                                isCompleted = isCompleted,
                                isTimerActive = isTimerActive,
                                isTimerPaused = isPaused,
                                remainingMs = if (isTimerActive) coordinatorState.remainingMs else 0L,
                                targetMs = if (isTimerActive) coordinatorState.targetMs else 0L,
                                isLoading = isTimerActive && coordinatorState.isLoading,
                                onCardClick = { onNavigateToHabitDetail(habit.id) },
                                onCompleteClick = {
                                    scope.launch {
                                        if (isTimerActive) {
                                            timerActionHandler?.handle(TimerIntent.Done, habit.id)
                                        } else {
                                            timerActionHandler?.handle(TimerIntent.QuickComplete, habit.id)
                                                ?: viewModel.markHabitComplete(habit.id)
                                        }
                                    }
                                },
                                onStartTimer = {
                                    scope.launch {
                                        timerActionHandler?.handle(TimerIntent.Start, habit.id)
                                    }
                                },
                                onPauseTimer = {
                                    scope.launch {
                                        timerActionHandler?.handle(TimerIntent.Pause, habit.id)
                                    }
                                },
                                onResumeTimer = {
                                    scope.launch {
                                        timerActionHandler?.handle(TimerIntent.Resume, habit.id)
                                    }
                                },
                                onCompleteWithTimer = {
                                    scope.launch {
                                        timerActionHandler?.handle(TimerIntent.Done, habit.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Mini session bar - positioned above bottom nav
            SimpleMiniSessionBar(
                isVisible = coordinatorState.trackedHabitId != null,
                habitName = activeHabit?.name ?: "",
                remainingMs = coordinatorState.remainingMs,
                isPaused = coordinatorState.paused,
                onPause = {
                    scope.launch {
                        coordinatorState.trackedHabitId?.let {
                            timerActionHandler?.handle(TimerIntent.Pause, it)
                        }
                    }
                },
                onResume = {
                    scope.launch {
                        coordinatorState.trackedHabitId?.let {
                            timerActionHandler?.handle(TimerIntent.Resume, it)
                        }
                    }
                },
                onComplete = {
                    scope.launch {
                        coordinatorState.trackedHabitId?.let {
                            timerActionHandler?.handle(TimerIntent.Done, it)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 88.dp)
            )
        }
        
        // Timer switcher sheet
        SimpleTimerSwitcherSheet(
            isVisible = showTimerSwitcher && pausedHabit != null,
            activeHabitName = activeHabit?.name ?: "",
            activeRemainingMs = coordinatorState.remainingMs,
            pausedHabitName = pausedHabit?.name ?: "",
            pausedRemainingMs = coordinatorState.pausedRemainingMs,
            onResumepaused = {
                scope.launch {
                    // Pause current, resume paused
                    coordinatorState.pausedHabitId?.let { pausedId ->
                        timerActionHandler?.handle(TimerIntent.Resume, pausedId)
                    }
                    showTimerSwitcher = false
                    timerActionHandler?.clearPausedHabit()
                }
            },
            onDismiss = {
                showTimerSwitcher = false
                timerActionHandler?.clearPausedHabit()
            }
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
