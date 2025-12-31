package com.habittracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.presentation.viewmodel.HabitViewModel
import com.habittracker.ui.models.timing.CompletionMetrics
import com.habittracker.ui.components.analytics.AnalyticsSummaryRow
import com.habittracker.ui.components.analytics.DurationSparkline
import com.habittracker.ui.viewmodels.analytics.HabitTimingAnalyticsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Duration
import com.habittracker.domain.isCompletedThisPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch
// Removed Canvas-based sparkline imports (using simple bar sparkline)

// ...existing code...

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HabitDetailScreen(
    habitId: Long,
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit
) {
    // Add safety check for invalid habitId
    if (habitId <= 0L) {
        // Invalid habitId, show error screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Habit Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Invalid habit ID")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }
    
    val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    // Hydrated UI models include analytics/timing; used for Analytics section below
    val hydrated by viewModel.hydratedHabits.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val habit = remember(habits, habitId) { 
        habits.find { it.id == habitId }
    }
    val habitUiModel = remember(hydrated, habitId) { 
        hydrated.find { it.id == habitId } 
    }
    
    if (habit == null) {
        // Show loading or error if habit not found
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Habit Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading habit details...")
                }
            }
        }
        return
    }
    
    val completedThisPeriod = isCompletedThisPeriod(habit.frequency, habit.lastCompletedDate)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit habit")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!completedThisPeriod) {
                        viewModel.markHabitComplete(habitId)
                    }
                },
                containerColor = if (completedThisPeriod) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                AnimatedContent(
                    targetState = completedThisPeriod,
                    transitionSpec = {
                        scaleIn() togetherWith scaleOut()
                    },
                    label = "fab_animation"
                ) { completed ->
                    if (completed) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Mark incomplete",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Mark complete",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Habit Header Card
            item(key = "header") {
                HabitHeaderCard(habit = habit, completedThisPeriod = completedThisPeriod)
            }
            
            // Streak Statistics
            item(key = "streak") {
                StreakStatisticsCard(habit = habit)
            }
            
            // Analytics Overview (Slice 3)
            item(key = "analytics") {
                AnalyticsOverviewCard(metrics = habitUiModel?.completionMetrics)
            }

            // UIX-6: Per-habit alert profile selector
            item(key = "alert_profile") {
                AlertProfileSelectorCard(
                    currentProfileId = habit.alertProfileId,
                    onChange = { newId -> viewModel.setHabitAlertProfile(habitId, newId) }
                )
            }

            // Phase UIX-8: Timing Analytics (sparkline & averages)
            item(key = "timing_analytics") {
                TimingSessionAnalyticsCard(habitId = habitId)
            }
            
            // Weekly Calendar View
            item(key = "weekly") {
                WeeklyProgressCard(habit = habit, onDateClick = { _ ->
                    // Future: Handle date-specific actions like showing details or editing
                })
            }
            
            // Monthly Overview
            item(key = "monthly") {
                MonthlyOverviewCard(habit = habit)
            }
            
            // Activity Log
            item(key = "activity") {
                ActivityLogCard(habit = habit)
            }
            
            // Add some space at the bottom for FAB
            item(key = "spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun AlertProfileSelectorCard(
    currentProfileId: String?,
    onChange: (String?) -> Unit
) {
    val profileVm: com.habittracker.ui.viewmodels.timing.AlertProfilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val ui by profileVm.ui.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Alert Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val display = ui.profiles.firstOrNull { it.id == (currentProfileId ?: ui.selectedId) }
                    Text(display?.displayName ?: (currentProfileId ?: ui.selectedId), style = MaterialTheme.typography.bodyMedium)
                    Text("Per-habit alert schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = { showDialog = true }, label = { Text("Change") })
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            title = { Text("Select alert profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Allow clearing to follow global default
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = { onChange(null); showDialog = false },
                                role = androidx.compose.ui.semantics.Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentProfileId == null, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use default (${ui.selectedId})")
                    }
                    ui.profiles.forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = { onChange(p.id); showDialog = false },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentProfileId == p.id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.displayName, fontWeight = FontWeight.Medium)
                                if (!p.description.isNullOrBlank()) Text(p.description!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TimingSessionAnalyticsCard(
    habitId: Long,
    sessionsToShow: Int = 14
) {
    // Add safety check for valid habitId
    if (habitId <= 0L) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Timing Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Invalid habit data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        return
    }
    
    val analyticsVm: HabitTimingAnalyticsViewModel = hiltViewModel()
    var hasLoadingError by remember { mutableStateOf(false) }
    
    LaunchedEffect(habitId) { 
        try {
            hasLoadingError = false
            analyticsVm.load(habitId) 
        } catch (e: Exception) {
            // Handle loading error gracefully
            hasLoadingError = true
            android.util.Log.e("TimingAnalytics", "Failed to load analytics for habit $habitId", e)
        }
    }
    
    val state by analyticsVm.uiState.collectAsStateWithLifecycle()

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Timing Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            when {
                hasLoadingError || state.error != null -> {
                    Text(
                        "Unable to load timing insights: ${state.error ?: "Loading error"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {
                    // Validate and prepare safe data for analytics display
                    val rawData = state.dataPoints.takeLast(sessionsToShow)
                    val isDataSafe = rawData.isNotEmpty() && 
                                   rawData.size >= 2 && 
                                   rawData.all { it > 0 && it < 10000 } // Reasonable bounds
                    
                    if (isDataSafe) {
                        // Additional safety validation before displaying analytics
                        val safeData = rawData.filter { it in 1..9999 }
                        val hasValidAnalytics = safeData.size >= 2 && 
                                              safeData.minOrNull() != null && 
                                              safeData.maxOrNull() != null
                        
                        if (hasValidAnalytics) {
                            // Safe to display analytics
                            DurationSparkline(
                                data = safeData,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            AnalyticsSummaryRow(
                                averageMinutes = state.averageDuration?.takeIf { it in 1..9999 },
                                targetMinutes = state.targetMinutes?.takeIf { it in 1..9999 },
                                adherencePercent = state.adherencePercent?.takeIf { it in 0..300 }
                            )
                            if (!state.suggestionReason.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text("Why this suggestion?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = state.suggestionReason ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                "Analytics data validation failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            "Timing insights appear after a couple of sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}@Composable
private fun Sparkline(values: List<Float>, modifier: Modifier = Modifier.height(40.dp).fillMaxWidth()) {
    if (values.isEmpty()) return
    val max = values.maxOrNull() ?: return
    val norm = values.map { (it / max).coerceIn(0f, 1f) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        norm.forEach { r ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(r)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitHeaderCard(
    habit: com.habittracker.data.database.entity.HabitEntity,
    completedThisPeriod: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (completedThisPeriod) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon from habit
                val iconVector = getIconById(habit.iconId)
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (habit.description.isNotBlank()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Completion status
                if (completedThisPeriod) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed today",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Frequency and creation date
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (completedThisPeriod) {
                    AssistChip(
                        onClick = { },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Completed this period") }
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
                
                AssistChip(
                    onClick = { },
                    label = { 
                        Text("Since ${java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(habit.createdDate)}")
                    }
                )
            }
        }
    }
}

@Composable
private fun StreakStatisticsCard(
    habit: com.habittracker.data.database.entity.HabitEntity
) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Streak Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    value = "${habit.streakCount}",
                    label = "Current Streak",
                    icon = Icons.Filled.LocalFireDepartment,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatisticItem(
                    value = "${habit.streakCount}",  // For now, using current as best
                    label = "Best Streak",
                    icon = Icons.Filled.EmojiEvents,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(
                    habit.createdDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
                ).toInt() + 1
                
                StatisticItem(
                    value = "$daysSinceStart",
                    label = "Total Days",
                    icon = Icons.Filled.CalendarToday,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeeklyProgressCard(
    habit: com.habittracker.data.database.entity.HabitEntity,
    onDateClick: (LocalDate) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "This Week",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val today = LocalDate.now()
            val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weekDays) { date ->
                    WeekDayItem(
                        date = date,
                        isCompleted = isDateCompleted(habit, date),
                        isToday = date == today,
                        onClick = { onDateClick(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayItem(
    date: LocalDate,
    isCompleted: Boolean,
    isToday: Boolean,
    @Suppress("UNUSED_PARAMETER") onClick: () -> Unit // Reserved for future date interaction
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthlyOverviewCard(
    habit: com.habittracker.data.database.entity.HabitEntity
) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Monthly Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val currentMonth = LocalDate.now().month
            val monthName = currentMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())
            
            // For now, showing a simple monthly stat
            // In a real app, you'd calculate actual completion data
            val daysInMonth = LocalDate.now().lengthOfMonth()
            val completedDays = minOf(habit.streakCount, daysInMonth)
            val completionFraction = (completedDays.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)
            val completionRate = (completionFraction * 100).toInt()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$completedDays/$daysInMonth days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$completionRate%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Completion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { completionFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ActivityLogCard(
    habit: com.habittracker.data.database.entity.HabitEntity
) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // For now, showing creation and last completion
            // In a real app, you'd have a proper activity log
            val activities = buildList {
                habit.lastCompletedDate?.let { lastCompleted ->
                    add(
                        ActivityItem(
                            title = "Completed",
                            subtitle = "Marked as done",
                            timestamp = java.util.Date.from(lastCompleted.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()),
                            icon = Icons.Filled.CheckCircle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                add(
                    ActivityItem(
                        title = "Habit Created",
                        subtitle = "Started tracking ${habit.name}",
                        timestamp = habit.createdDate,
                        icon = Icons.Filled.Add,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )
            }
            
            activities.forEach { activity ->
                ActivityItemRow(activity)
                if (activity != activities.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            if (activities.isEmpty()) {
                Text(
                    text = "No activity yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class ActivityItem(
    val title: String,
    val subtitle: String,
    val timestamp: Date,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
private fun ActivityItemRow(activity: ActivityItem) {
    val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = activity.icon,
            contentDescription = null,
            tint = activity.color,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = formatter.format(activity.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalyticsOverviewCard(
    metrics: CompletionMetrics?
) {
    Card {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (metrics == null) {
                Text(
                    text = "Insights appear after a few completions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            // Row of three key insights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Consistency %
                InsightStat(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Consistency",
                    value = "${(metrics.consistencyScore * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.primary,
                    contentDescription = "Consistency ${(metrics.consistencyScore * 100).toInt()} percent"
                )

                // Avg Session Duration
                val avgDur = metrics.averageSessionDuration
                InsightStat(
                    icon = Icons.Filled.Timer,
                    label = "Avg session",
                    value = formatDurationShort(avgDur),
                    color = MaterialTheme.colorScheme.secondary,
                    contentDescription = avgDur?.let { "Average session ${formatDurationShort(it)}" } ?: "Average session duration not available"
                )

                // Best Time Band
                val bestSlot = metrics.optimalTimeSlots.maxWithOrNull(compareBy<com.habittracker.ui.models.timing.TimeSlot>({ it.successRate }, { it.sampleSize }))
                InsightStat(
                    icon = Icons.Filled.Schedule,
                    label = "Best time",
                    value = bestSlot?.let { formatTimeBand(it.startTime, it.endTime) } ?: "—",
                    color = MaterialTheme.colorScheme.tertiary,
                    contentDescription = bestSlot?.let { "Best time ${formatTimeBand(it.startTime, it.endTime)}" } ?: "Best time not available"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Consistency reflects how regularly you complete this habit around the same times.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    contentDescription: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 80.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDurationShort(duration: Duration?): String {
    if (duration == null) return "—"
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        mins > 0 -> "${mins}m"
        else -> "<1m"
    }
}

private fun formatTimeBand(start: java.time.LocalTime, end: java.time.LocalTime): String {
    val fmt = DateTimeFormatter.ofPattern("h a")
    val startStr = start.format(fmt)
    val endStr = end.format(fmt)
    return "$startStr–$endStr"
}

// Helper functions
private fun isDateCompleted(
    habit: com.habittracker.data.database.entity.HabitEntity,
    date: LocalDate
): Boolean {
    // For now, simple logic - in a real app you'd check completion history
    return habit.lastCompletedDate == date
}

private fun getIconById(iconId: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconId) {
        1 -> Icons.Filled.FitnessCenter
        2 -> Icons.Filled.LocalDrink
    3 -> Icons.AutoMirrored.Filled.MenuBook
    4 -> Icons.AutoMirrored.Filled.DirectionsRun
        5 -> Icons.Filled.Bedtime
        6 -> Icons.Filled.Restaurant
        7 -> Icons.Filled.School
        8 -> Icons.Filled.Work
        9 -> Icons.Filled.Favorite
        10 -> Icons.Filled.SelfImprovement
        11 -> Icons.Filled.MusicNote
        12 -> Icons.Filled.Brush
        13 -> Icons.Filled.Code
        14 -> Icons.Filled.Language
        15 -> Icons.Filled.Savings
        16 -> Icons.Filled.CleanHands
        17 -> Icons.Filled.Eco
        18 -> Icons.Filled.Group
        19 -> Icons.Filled.Timer
        20 -> Icons.Filled.Star
        else -> Icons.Filled.Circle
    }
}
