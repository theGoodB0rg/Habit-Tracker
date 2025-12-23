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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.habittracker.ui.models.HabitUiModel
import com.habittracker.onboarding.components.rememberTooltipTarget
import com.habittracker.ui.models.timing.HabitTiming
import com.habittracker.domain.isCompletedThisPeriod
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HabitCard(
    habit: HabitUiModel,
    onMarkComplete: () -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val completedFromModel = isCompletedThisPeriod(habit.frequency, habit.lastCompletedDate)
    var completedThisPeriod by remember(habit.id) { mutableStateOf(completedFromModel) }
    LaunchedEffect(completedFromModel) {
        completedThisPeriod = completedFromModel
    }
    val hasAnyCompletion = habit.lastCompletedDate != null
    
    // Date formatter for future use (e.g., showing last completed date)
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (hasAnyCompletion) {
                    onClick()
                } else {
                    // No navigation when there are no completions yet
                }
            }
            .then(rememberTooltipTarget("habit_card")),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (completedThisPeriod) 6.dp else 3.dp,
            pressedElevation = if (completedThisPeriod) 8.dp else 5.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (completedThisPeriod) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp) // Modern rounded corners
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 12.dp else 16.dp,
                vertical = if (isCompact) 12.dp else 16.dp
            )
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = if (isCompact) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!isCompact && habit.description.isNotBlank()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp), // Increased spacing
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Show last completed date if available with better spacing
                    if (!isCompact && habit.lastCompletedDate != null && !completedThisPeriod) {
                        val lastCompletedText = remember(habit.lastCompletedDate) {
                            "Last: ${dateFormatter.format(java.sql.Date.valueOf(habit.lastCompletedDate.toString()))}"
                        }
                        Text(
                            text = lastCompletedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp) // Better visual hierarchy
                        )
                    }
                }
                
                // Quick actions
                Row {
                    if (!isCompact) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit habit",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Mark complete button
                    IconButton(
                        onClick = {
                            completedThisPeriod = true
                            onMarkComplete()
                        },
                        enabled = !completedThisPeriod,
                        modifier = Modifier
                            .size(40.dp)
                            .then(rememberTooltipTarget("habit_complete_button"))
                    ) {
                        AnimatedContent(
                            targetState = completedThisPeriod,
                            transitionSpec = {
                                scaleIn() togetherWith scaleOut()
                            },
                            label = "completion_animation"
                        ) { completed ->
                            // Larger, more prominent completion indicator
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .then(
                                        if (completed) {
                                            Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (completed) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = if (completed) "Completed this period" else "Mark complete",
                                    tint = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(if (completed) 20.dp else 28.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            if (!isCompact) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    
                    // Timer required indicator
                    if (habit.timing?.requireTimerToComplete == true) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = { 
                                Icon(
                                    Icons.Filled.Timer, 
                                    contentDescription = "Timer required", 
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = "Timer Required",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Timing chips (preferred time / duration / live)
                    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
                    if (habit.preferredTime != null && habit.hasSchedule) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = habit.preferredTime!!.format(timeFmt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                    if (habit.estimatedDuration != null && habit.hasTimer) {
                        val mins = habit.estimatedDuration!!.toMinutes()
                        val durLabel = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                        AssistChip(
                            onClick = { },
                            leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = durLabel,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                    if (habit.isTimerActive) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = "Live",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Top smart suggestion (if any)
                    val topSuggestion = habit.highestConfidenceSuggestion
                    if (topSuggestion != null) {
                        val label = when {
                            topSuggestion.suggestedTime != null -> "Try " + topSuggestion.suggestedTime.format(timeFmt)
                            topSuggestion.suggestedDuration != null -> {
                                val m = topSuggestion.suggestedDuration.toMinutes()
                                if (m >= 60) "Try ${m / 60}h ${m % 60}m" else "Try ${m}m"
                            }
                            else -> "Suggestion"
                        }
                        AssistChip(
                            onClick = { },
                            leadingIcon = { Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }

                    // Average completion time (if any metrics available)
                    val avgTime = habit.completionMetrics?.averageCompletionTime
                    if (avgTime != null) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = "Avg " + avgTime.format(timeFmt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
                
                // Last completion
                habit.lastCompletedDate?.let { lastCompleted ->
                    Text(
                        text = "Last: ${lastCompleted.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // Compact view stats
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 ${habit.streakCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
