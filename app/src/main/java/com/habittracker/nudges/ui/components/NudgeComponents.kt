package com.habittracker.nudges.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.habittracker.nudges.model.Nudge
import com.habittracker.nudges.model.NudgePriority
import com.habittracker.nudges.model.NudgeType

/**
 * Displays a single nudge card with appropriate styling and actions
 * Enhanced for responsiveness, proper spacing, and overflow protection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeCard(
    nudge: Nudge,
    onDismiss: (String) -> Unit,
    onActionTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = getNudgeColors(nudge.priority, nudge.type)
    val icon = getNudgeIcon(nudge.type)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 4.dp, vertical = 2.dp) // Consistent outer spacing
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            // Priority indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(colors.accentColor, colors.accentColor.copy(alpha = 0.6f))
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    )
            ) {
                // Header with icon and dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colors.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = nudge.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textColor,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    IconButton(
                        onClick = { onDismiss(nudge.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = colors.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Message with proper text wrapping and overflow protection
                Text(
                    text = nudge.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textColor.copy(alpha = 0.85f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.2f),
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Action button if available
                if (nudge.actionText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onActionTaken(nudge.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Text(
                            text = nudge.actionText,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays a floating nudge overlay that appears on top of other content
 * Enhanced for maximum responsiveness and professional presentation
 */
@Composable
fun FloatingNudgeOverlay(
    nudge: Nudge,
    onDismiss: (String) -> Unit,
    onActionTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = getNudgeColors(nudge.priority, nudge.type)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box {
            // Animated background gradient for visual depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getNudgeIcon(nudge.type),
                                contentDescription = null,
                                tint = colors.accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = nudge.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textColor,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = nudge.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textColor.copy(alpha = 0.9f),
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.3f),
                            maxLines = 5,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    IconButton(
                        onClick = { onDismiss(nudge.id) },
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = colors.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                if (nudge.actionText != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss(nudge.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.textColor.copy(alpha = 0.7f)
                            ),
                            border = BorderStroke(1.5.dp, colors.textColor.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Maybe Later",
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        Button(
                            onClick = { onActionTaken(nudge.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Text(
                                text = nudge.actionText,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact nudge banner for subtle notifications
 * Enhanced for perfect responsiveness and overflow protection
 */
@Composable
fun NudgeBanner(
    nudge: Nudge,
    onDismiss: (String) -> Unit,
    onActionTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = getNudgeColors(nudge.priority, nudge.type)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        color = colors.backgroundColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getNudgeIcon(nudge.type),
                contentDescription = null,
                tint = colors.accentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = nudge.message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textColor,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.2f)
            )
            
            if (nudge.actionText != null) {
                TextButton(
                    onClick = { onActionTaken(nudge.id) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.accentColor
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = nudge.actionText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            IconButton(
                onClick = { onDismiss(nudge.id) },
                modifier = Modifier
                    .size(32.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = colors.textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Data class for nudge colors based on priority and type
 */
private data class NudgeColors(
    val backgroundColor: Color,
    val accentColor: Color,
    val textColor: Color
)

/**
 * Gets appropriate colors for a nudge based on its priority and type
 */
@Composable
private fun getNudgeColors(priority: NudgePriority, type: NudgeType): NudgeColors {
    val colorScheme = MaterialTheme.colorScheme
    
    return when (priority) {
        NudgePriority.CRITICAL -> NudgeColors(
            backgroundColor = colorScheme.errorContainer,
            accentColor = colorScheme.error,
            textColor = colorScheme.onErrorContainer
        )
        NudgePriority.HIGH -> NudgeColors(
            backgroundColor = colorScheme.tertiaryContainer,
            accentColor = colorScheme.tertiary,
            textColor = colorScheme.onTertiaryContainer
        )
        NudgePriority.MEDIUM -> when (type) {
            NudgeType.CELEBRATION -> NudgeColors(
                backgroundColor = Color(0xFFF3E5F5), // Light purple
                accentColor = Color(0xFF9C27B0), // Purple
                textColor = Color(0xFF4A148C)
            )
            else -> NudgeColors(
                backgroundColor = colorScheme.secondaryContainer,
                accentColor = colorScheme.secondary,
                textColor = colorScheme.onSecondaryContainer
            )
        }
        NudgePriority.LOW -> NudgeColors(
            backgroundColor = colorScheme.primaryContainer,
            accentColor = colorScheme.primary,
            textColor = colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Gets appropriate icon for a nudge type
 */
private fun getNudgeIcon(type: NudgeType): ImageVector {
    return when (type) {
        NudgeType.STREAK_BREAK_WARNING -> Icons.Default.Warning
        NudgeType.MOTIVATIONAL_QUOTE -> Icons.Default.Psychology
        NudgeType.EASIER_GOAL_SUGGESTION -> Icons.Outlined.Lightbulb
        NudgeType.CELEBRATION -> Icons.Default.Celebration
        NudgeType.REMINDER -> Icons.Default.Notifications
        NudgeType.TIP_OF_THE_DAY -> Icons.Default.TipsAndUpdates
    }
}
