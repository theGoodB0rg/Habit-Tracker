package com.habittracker.ui.components.timing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habittracker.ui.models.timing.UserEngagementLevel
import com.habittracker.ui.models.timing.Feature
import com.habittracker.ui.models.HabitUiModel
import com.habittracker.ui.models.timing.HabitTiming
import com.habittracker.ui.models.timing.TimerSession
import com.habittracker.ui.models.timing.Break
import com.habittracker.ui.models.timing.BreakType
import com.habittracker.ui.models.timing.ReminderStyle
import com.habittracker.ui.models.timing.ContextType
import com.habittracker.ui.models.timing.SmartSuggestion
import com.habittracker.ui.models.timing.SuggestionType
import com.habittracker.ui.models.timing.TimerType
import com.habittracker.ui.models.timing.CompletionMetrics
import com.habittracker.utils.TimerDisplayUtils
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Phase 2: Progressive UI Complexity System
 * 
 * Level 0: Invisible (Default) - No timing UI shown
 * Level 1: Simple Timer - Single "Start Timer" button
 * Level 2: Smart Suggestions - Gentle timing recommendations
 * Level 3: Full Intelligence - Complete timing suite
 */

// LEVEL 0: Invisible (Default)
// No components needed - existing UI unchanged

// LEVEL 1: Simple Timer Button
@Composable
fun SimpleTimerButton(
    habit: HabitUiModel,
    onStartTimer: (HabitUiModel, Duration) -> Unit,
    modifier: Modifier = Modifier
) {
    // Best of both: keep it visible but compact, and respect feature preference when available.
    val timingViewModel: com.habittracker.ui.viewmodels.timing.TimingFeatureViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val prefs by timingViewModel.smartTimingPreferences.collectAsState()
    val timersEnabled = timingViewModel.isFeatureEnabled(com.habittracker.ui.models.timing.Feature.SIMPLE_TIMER)
    val defaultDuration = habit.estimatedDuration ?: Duration.ofMinutes(25)

    AssistChip(
        onClick = {
            if (!timersEnabled) {
                // One-tap enablement: turn on SIMPLE_TIMER and proceed.
                timingViewModel.enableFeature(com.habittracker.ui.models.timing.Feature.SIMPLE_TIMER)
            }
            onStartTimer(habit, defaultDuration)
        },
        label = { Text(text = if (timersEnabled) "Start" else "Start (Enable)" ) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = if (timersEnabled) "Start timer" else "Timer disabled, enable in settings",
                modifier = Modifier.size(16.dp)
            )
        },
        enabled = true,
        modifier = modifier
    )
}

@Composable
fun LiveRemainingTime(
    habitId: Long,
    modifier: Modifier = Modifier,
    timerViewModel: com.habittracker.ui.viewmodels.timing.TimerTickerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val remainingByHabit by timerViewModel.remainingByHabit.collectAsState()
    val remaining = remainingByHabit[habitId]
    if (remaining != null) {
        val timeText = TimerDisplayUtils.formatTime(remaining)
        
        AssistChip(
            onClick = { },
            label = { Text(text = timeText) },
            modifier = modifier.semantics {
                contentDescription = "Timer ${TimerDisplayUtils.formatTimeForAccessibility(remaining)} remaining"
                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
            }
        )
    }
}

// LEVEL 2: Smart Suggestion Card
@Composable
fun SmartSuggestionCard(
    suggestion: SmartSuggestion,
    onApplySuggestion: (SmartSuggestion) -> Unit,
    onDismissSuggestion: (SmartSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = getSuggestionIcon(suggestion.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "💡 Smart Tip",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                TextButton(
                    onClick = { onApplySuggestion(suggestion) }
                ) {
                    Text("Try It")
                }
                IconButton(
                    onClick = { onDismissSuggestion(suggestion) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// LEVEL 3: Advanced Timing Controls
@Composable
fun AdvancedTimingControls(
    habit: HabitUiModel,
    onTimerTypeSelected: (TimerType) -> Unit,
    onStartTimer: (Duration) -> Unit,
    onScheduleOptimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Timing Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timer Type Selector
            TimerTypeSelector(
                currentType = habit.timerSession?.type ?: TimerType.SIMPLE,
                onTypeSelected = onTimerTypeSelected
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Schedule Optimizer
            if (habit.completionMetrics != null) {
                ScheduleOptimizer(
                    metrics = habit.completionMetrics,
                    onOptimize = onScheduleOptimize
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Context Awareness Panel
            ContextAwarenessPanel(
                suggestions = habit.smartSuggestions
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Habit Stacking Recommendations
            HabitStackingRecommendations(habit = habit)
        }
    }
}

@Composable
private fun TimerTypeSelector(
    currentType: TimerType,
    onTypeSelected: (TimerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Timer Type",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TimerType.values().forEach { type ->
                FilterChip(
                    selected = currentType == type,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            text = type.displayName(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ScheduleOptimizer(
    metrics: CompletionMetrics,
    onOptimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Schedule Optimizer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    metrics.averageCompletionTime?.let { avgTime ->
                        Text(
                            text = "Optimal time: ${avgTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = onOptimize,
                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        text = "Optimize",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            // Efficiency Score Display
            if (metrics.efficiencyScore > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { metrics.efficiencyScore },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        metrics.efficiencyScore >= 0.8f -> MaterialTheme.colorScheme.primary
                        metrics.efficiencyScore >= 0.6f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = "Efficiency: ${(metrics.efficiencyScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ContextAwarenessPanel(
    suggestions: List<SmartSuggestion>,
    modifier: Modifier = Modifier
) {
    if (suggestions.isNotEmpty()) {
        Column(modifier = modifier) {
            Text(
                text = "Context Awareness",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            suggestions.take(2).forEach { suggestion ->
                ContextSuggestionChip(suggestion = suggestion)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ContextSuggestionChip(
    suggestion: SmartSuggestion,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { /* Handle context suggestion */ },
        label = {
            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = getContextIcon(suggestion.type),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun HabitStackingRecommendations(
    habit: HabitUiModel,
    modifier: Modifier = Modifier
) {
    // Placeholder for habit stacking UI
    val stackingSuggestions = remember { 
        // This would come from HabitStackingEngine in real implementation
        listOf(
            "Stack with: Morning Stretch",
            "Try after: Coffee routine"
        )
    }
    
    if (stackingSuggestions.isNotEmpty()) {
        Column(modifier = modifier) {
            Text(
                text = "Habit Stacking",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            stackingSuggestions.forEach { suggestion ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// Progressive Feature Discovery Component
@Composable
fun ProgressiveTimingDiscovery(
    habit: HabitUiModel,
    userEngagementLevel: UserEngagementLevel,
    onLevelUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = shouldShowTimingFeatures(habit, userEngagementLevel),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        when (userEngagementLevel) {
            UserEngagementLevel.Casual -> {
                // Level 0: Show nothing extra
            }
            
            UserEngagementLevel.Interested -> {
                // Level 1: Primary Start chip is surfaced by the card itself; avoid duplicating here.
            }
            
            UserEngagementLevel.Engaged -> {
                // Level 2: + Smart suggestions
                habit.smartSuggestions.firstOrNull()?.let { suggestion ->
                    SmartSuggestionCard(
                        suggestion = suggestion,
                        onApplySuggestion = { /* Handle apply */ },
                        onDismissSuggestion = { /* Handle dismiss */ }
                    )
                }
            }
            
            UserEngagementLevel.PowerUser -> {
                // Level 3: Full controls
                AdvancedTimingControls(
                    habit = habit,
                    onTimerTypeSelected = { /* Handle type selection */ },
                    onStartTimer = { /* Handle start */ },
                    onScheduleOptimize = { /* Handle optimization */ }
                )
            }
        }
    }
}

// Helper Functions
private fun shouldShowTimingFeatures(habit: HabitUiModel, level: UserEngagementLevel): Boolean {
    return when (level) {
        UserEngagementLevel.Casual -> false
        UserEngagementLevel.Interested -> habit.timing?.timerEnabled == true
        UserEngagementLevel.Engaged -> habit.smartSuggestions.isNotEmpty() || habit.timing?.timerEnabled == true
        UserEngagementLevel.PowerUser -> true
    }
}

private fun getSuggestionIcon(type: SuggestionType): ImageVector {
    return when (type) {
        SuggestionType.OPTIMAL_TIME -> Icons.Default.Schedule
        SuggestionType.OPTIMAL_DURATION -> Icons.Default.Timer
        SuggestionType.DURATION_ADJUSTMENT -> Icons.Default.Timer
        SuggestionType.BREAK_OPTIMIZATION -> Icons.Default.Coffee
        SuggestionType.CONTEXT_OPPORTUNITY -> Icons.Default.Psychology
        SuggestionType.EFFICIENCY_BOOST -> Icons.Default.BatteryChargingFull
        SuggestionType.HABIT_PAIRING -> Icons.Default.Link
        SuggestionType.RECOVERY_TIME -> Icons.Default.RestoreFromTrash
        SuggestionType.HABIT_STACKING -> Icons.Default.Link
        SuggestionType.CONTEXT_OPTIMIZATION -> Icons.Default.Psychology
        SuggestionType.ENERGY_ALIGNMENT -> Icons.Default.BatteryChargingFull
        SuggestionType.SCHEDULE_OPTIMIZATION -> Icons.Default.Schedule
        SuggestionType.WEATHER_ALTERNATIVE -> Icons.Default.Cloud
    }
}

private fun getContextIcon(type: SuggestionType): ImageVector {
    return when (type) {
        SuggestionType.OPTIMAL_TIME -> Icons.Default.AccessTime
        SuggestionType.OPTIMAL_DURATION -> Icons.Default.Timer
        SuggestionType.DURATION_ADJUSTMENT -> Icons.Default.Timer
        SuggestionType.BREAK_OPTIMIZATION -> Icons.Default.PauseCircle
        SuggestionType.CONTEXT_OPPORTUNITY -> Icons.Default.LocationOn
        SuggestionType.EFFICIENCY_BOOST -> Icons.Default.TrendingUp
        SuggestionType.HABIT_PAIRING -> Icons.Default.Link
        SuggestionType.RECOVERY_TIME -> Icons.Default.Refresh
        SuggestionType.HABIT_STACKING -> Icons.Default.Link
        SuggestionType.CONTEXT_OPTIMIZATION -> Icons.Default.LocationOn
        SuggestionType.ENERGY_ALIGNMENT -> Icons.Default.TrendingUp
        SuggestionType.SCHEDULE_OPTIMIZATION -> Icons.Default.AccessTime
        SuggestionType.WEATHER_ALTERNATIVE -> Icons.Default.Cloud
    }
}

private fun TimerType.displayName(): String {
    return when (this) {
        TimerType.SIMPLE -> "Simple"
        TimerType.POMODORO -> "Pomodoro"
        TimerType.INTERVAL -> "Interval"
        TimerType.PROGRESSIVE -> "Progressive"
        TimerType.CUSTOM -> "Custom"
        TimerType.FLEXIBLE -> "Flexible"
        TimerType.FOCUS_SESSION -> "Focus Session"
    }
}

private fun TimerType.icon(): ImageVector {
    return when (this) {
        TimerType.SIMPLE -> Icons.Default.Timer
        TimerType.POMODORO -> Icons.Default.Schedule
        TimerType.INTERVAL -> Icons.Default.Repeat
        TimerType.PROGRESSIVE -> Icons.Default.TrendingUp
        TimerType.CUSTOM -> Icons.Default.Settings
        TimerType.FLEXIBLE -> Icons.Default.Speed
        TimerType.FOCUS_SESSION -> Icons.Default.Psychology
    }
}
