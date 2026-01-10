package com.habittracker.nudges.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.nudges.model.NudgePriority
import com.habittracker.nudges.ui.components.FloatingNudgeOverlay
import com.habittracker.nudges.ui.components.NudgeBanner
import com.habittracker.nudges.ui.components.NudgeCard
import com.habittracker.nudges.viewmodel.NudgeViewModel

/**
 * Overlay component that displays high-priority nudges as floating dialogs
 * Enhanced for responsive design and overflow protection
 */
@Composable
fun NudgeOverlay(
    viewModel: NudgeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val activeNudges by viewModel.activeNudges.collectAsStateWithLifecycle()
    
    // Show critical nudges as modal dialogs
    val criticalNudges = activeNudges.filter { it.priority == NudgePriority.CRITICAL }
    
    // Show high priority nudges as floating overlays
    val highPriorityNudges = activeNudges.filter { it.priority == NudgePriority.HIGH }
    
    // Critical nudges (modal dialogs) - Enhanced with proper responsiveness
    criticalNudges.firstOrNull()?.let { nudge ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissNudge(nudge.id) },
            title = { 
                Text(
                    text = nudge.title,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    text = nudge.message,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.3f)
                )
            },
            confirmButton = {
                nudge.actionText?.let { actionText ->
                    Button(
                        onClick = { viewModel.takeNudgeAction(nudge.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = actionText,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissNudge(nudge.id) }
                ) {
                    Text("Dismiss")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    // High priority nudges (floating overlays) - Enhanced positioning
    highPriorityNudges.firstOrNull()?.let { nudge ->
        Box(
            modifier = modifier.padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            FloatingNudgeOverlay(
                nudge = nudge,
                onDismiss = { viewModel.dismissNudge(it) },
                onActionTaken = { viewModel.takeNudgeAction(it) }
            )
        }
    }
}

/**
 * Banner section that displays medium/low priority nudges as compact banners
 * Enhanced for seamless scrolling and responsive design
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NudgeBannerSection(
    viewModel: NudgeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val activeNudges by viewModel.activeNudges.collectAsStateWithLifecycle()
    
    // Filter for banner-appropriate nudges (medium and low priority)
    val bannerNudges = activeNudges.filter { 
        it.priority == NudgePriority.MEDIUM || it.priority == NudgePriority.LOW
    }.take(3) // Limit to 3 banners for optimal user experience
    
    Crossfade(
        targetState = bannerNudges,
        label = "nudge_banners"
    ) { nudges ->
        if (nudges.isNotEmpty()) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nudges.forEach { nudge ->
                    NudgeBanner(
                        nudge = nudge,
                        onDismiss = { viewModel.dismissNudge(it) },
                        onActionTaken = { viewModel.takeNudgeAction(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Comprehensive nudge management screen for advanced users
 * Fully responsive with proper scrolling and overflow protection
 */
@Composable
fun NudgeManagementScreen(
    viewModel: NudgeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val activeNudges by viewModel.activeNudges.collectAsStateWithLifecycle()
    val nudgeStats by viewModel.nudgeStats.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation header if callback provided
        onNavigateBack?.let { callback ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = callback,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nudge Management",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Stats header with enhanced spacing
        item {
            NudgeStatsCard(
                stats = nudgeStats,
                onDismissAll = { viewModel.dismissAllNudges() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Active nudges by priority with better organization
        if (activeNudges.isNotEmpty()) {
            val nudgesByPriority = activeNudges.groupBy { it.priority }
            
            nudgesByPriority.forEach { (priority, nudges) ->
                item {
                    Text(
                        text = "${priority.name} Priority (${nudges.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            top = 8.dp,
                            bottom = 4.dp
                        )
                    )
                }
                
                items(
                    items = nudges,
                    key = { it.id }
                ) { nudge ->
                    NudgeCard(
                        nudge = nudge,
                        onDismiss = { viewModel.dismissNudge(it) },
                        onActionTaken = { viewModel.takeNudgeAction(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            item {
                EmptyNudgesState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        }
    }
}

/**
 * Statistics card showing nudge engagement metrics
 * Enhanced for responsive design and proper spacing
 */
@Composable
private fun NudgeStatsCard(
    stats: com.habittracker.nudges.repository.NudgeStats,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Nudge Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Your motivation overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                if (stats.activeNudges > 0) {
                    TextButton(
                        onClick = onDismissAll,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Dismiss All",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Active",
                    value = stats.activeNudges.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Engagement",
                    value = "${(stats.engagementRate * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Total",
                    value = stats.totalNudges.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual statistic item with enhanced responsiveness
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/**
 * Empty state when no nudges are active
 * Enhanced with better spacing and visual appeal
 */
@Composable
private fun EmptyNudgesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 32.dp,
                vertical = 48.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Active Nudges",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You're all caught up! Keep building those habits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.2f),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
    }
}