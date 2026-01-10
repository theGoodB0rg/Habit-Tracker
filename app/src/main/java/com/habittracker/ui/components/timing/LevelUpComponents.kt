package com.habittracker.ui.components.timing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.habittracker.ui.models.timing.UserEngagementLevel
import com.habittracker.ui.models.timing.Feature
import com.habittracker.ui.models.timing.LevelUpNotification
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.habittracker.ui.models.timing.*

/**
 * Phase 2: Progressive Discovery UI Components
 * 
 * Smooth, non-intrusive level-up system that celebrates user progress
 * and introduces new features gradually
 */

@Composable
fun LevelUpDialog(
    notification: LevelUpNotification,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration Header
                LevelUpHeader(
                    fromLevel = notification.fromLevel,
                    toLevel = notification.toLevel
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Congratulations Message
                Text(
                    text = notification.congratsMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // New Features Preview
                NewFeaturesPreview(
                    features = notification.unlockedFeatures,
                    benefits = notification.benefits
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Maybe Later")
                    }
                    
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Unlock Features")
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelUpHeader(
    fromLevel: UserEngagementLevel,
    toLevel: UserEngagementLevel,
    modifier: Modifier = Modifier
) {
    // Animated star burst effect
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Main icon with animation
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Icon(
                imageVector = getLevelIcon(toLevel),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Sparkle effects
        SparkleEffect(visible = visible)
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
        text = "Level Up!",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    
    Text(
        text = "${fromLevel.displayName()} → ${toLevel.displayName()}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SparkleEffect(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val sparkles = remember {
        listOf(
            SparkleData(x = -30.dp, y = -20.dp, delay = 0),
            SparkleData(x = 25.dp, y = -30.dp, delay = 100),
            SparkleData(x = -35.dp, y = 15.dp, delay = 200),
            SparkleData(x = 30.dp, y = 20.dp, delay = 300),
            SparkleData(x = 0.dp, y = -40.dp, delay = 150),
            SparkleData(x = -15.dp, y = 35.dp, delay = 250)
        )
    }
    
    sparkles.forEach { sparkle ->
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = sparkle.delay,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    delayMillis = sparkle.delay
                )
            ),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = modifier.offset(x = sparkle.x, y = sparkle.y)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NewFeaturesPreview(
    features: List<Feature>,
    benefits: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "New Features Unlocked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            benefits.forEach { benefit ->
                FeatureBenefitRow(benefit = benefit)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FeatureBenefitRow(
    benefit: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = benefit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Feature Discovery Banner - Subtle promotion of next level
@Composable
fun FeatureDiscoveryBanner(
    currentLevel: UserEngagementLevel,
    nextLevelBenefits: List<String>,
    progressToNextLevel: Float,
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    
    AnimatedVisibility(
        visible = visible && nextLevelBenefits.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock More Features",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            visible = false
                            onDismiss()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { progressToNextLevel },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = nextLevelBenefits.firstOrNull() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLearnMore,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Learn More",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

// Timing Feature Introduction Card
@Composable
fun TimingFeatureIntroCard(
    feature: Feature,
    isNewlyUnlocked: Boolean = false,
    onTryFeature: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isNewlyUnlocked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "background_color"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFeatureIcon(feature),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = feature.displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isNewlyUnlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = feature.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column {
                TextButton(onClick = onTryFeature) {
                    Text("Try It")
                }
            }
        }
    }
}

// Helper Functions
private data class SparkleData(
    val x: androidx.compose.ui.unit.Dp,
    val y: androidx.compose.ui.unit.Dp,
    val delay: Int
)

private fun getLevelIcon(level: UserEngagementLevel): ImageVector {
    return when (level) {
        UserEngagementLevel.Casual -> Icons.Default.Person
        UserEngagementLevel.Interested -> Icons.Default.Timer
        UserEngagementLevel.Engaged -> Icons.Default.Psychology
        UserEngagementLevel.PowerUser -> Icons.Default.Star
    }
}

private fun UserEngagementLevel.displayName(): String {
    return when (this) {
        UserEngagementLevel.Casual -> "Casual"
        UserEngagementLevel.Interested -> "Interested"
        UserEngagementLevel.Engaged -> "Engaged"
        UserEngagementLevel.PowerUser -> "Power User"
    }
}

private fun getFeatureIcon(feature: Feature): ImageVector {
    return when (feature) {
        Feature.BASIC_TRACKING -> Icons.Default.CheckCircle
        Feature.SIMPLE_TIMER -> Icons.Default.Timer
        Feature.SMART_SUGGESTIONS -> Icons.Default.Psychology
        Feature.SCHEDULE_OPTIMIZATION -> Icons.Default.Schedule
        Feature.CONTEXT_AWARENESS -> Icons.Default.Visibility
        Feature.HABIT_STACKING -> Icons.Default.Link
        Feature.ADVANCED_ANALYTICS -> Icons.Default.Analytics
        Feature.ENERGY_OPTIMIZATION -> Icons.Default.BatteryChargingFull
        Feature.FOCUS_ENHANCEMENT -> Icons.Default.Headphones
        Feature.CALENDAR_INTEGRATION -> Icons.Default.CalendarToday
    }
}

private fun Feature.displayName(): String {
    return when (this) {
        Feature.BASIC_TRACKING -> "Basic Tracking"
        Feature.SIMPLE_TIMER -> "Simple Timer"
        Feature.SMART_SUGGESTIONS -> "Smart Suggestions"
        Feature.SCHEDULE_OPTIMIZATION -> "Schedule Optimizer"
        Feature.CONTEXT_AWARENESS -> "Context Awareness"
        Feature.HABIT_STACKING -> "Habit Stacking"
        Feature.ADVANCED_ANALYTICS -> "Advanced Analytics"
        Feature.ENERGY_OPTIMIZATION -> "Energy Optimization"
        Feature.FOCUS_ENHANCEMENT -> "Focus Enhancement"
        Feature.CALENDAR_INTEGRATION -> "Calendar Integration"
    }
}

private fun Feature.description(): String {
    return when (this) {
        Feature.BASIC_TRACKING -> "Track your habits and build streaks"
        Feature.SIMPLE_TIMER -> "Focus with one-tap 25-minute timers"
        Feature.SMART_SUGGESTIONS -> "AI-powered timing recommendations"
        Feature.SCHEDULE_OPTIMIZATION -> "Find your optimal habit times"
        Feature.CONTEXT_AWARENESS -> "Environment-based suggestions"
        Feature.HABIT_STACKING -> "Combine habits for better results"
        Feature.ADVANCED_ANALYTICS -> "Detailed insights and patterns"
        Feature.ENERGY_OPTIMIZATION -> "Match habits to your energy levels"
        Feature.FOCUS_ENHANCEMENT -> "Ambient sounds and distraction blocking"
        Feature.CALENDAR_INTEGRATION -> "Sync with your calendar app"
    }
}
