package com.habittracker.analytics.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.analytics.domain.models.*
import com.habittracker.analytics.presentation.viewmodel.*
import kotlin.math.*

/**
 * Comprehensive UI components for analytics visualization with modern Material 3 design
 */

@Composable
fun AnalyticsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                action?.invoke()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            content()
        }
    }
}

@Composable
fun CompletionRateChart(
    data: List<CompletionRateChartPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(
            message = "No completion data available",
            modifier = modifier
        )
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
        )
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.8f
            val strokeWidth = 20.dp.toPx()
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Completion rate arcs
            var currentAngle = -90f
            data.forEach { habit ->
                val sweepAngle = (habit.completionRate / 100f) * 360f * animatedProgress.value
                val color = Color(habit.color)
                
                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                
                currentAngle += sweepAngle
            }
        }
        
        // Center text with average completion
        val avgCompletion = (data.map { it.completionRate }.average() * animatedProgress.value).toInt()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$avgCompletion%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Average",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Legend
    CompletionRateLegend(data = data)
}

@Composable
private fun CompletionRateLegend(
    data: List<CompletionRateChartPoint>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { habit ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(habit.color))
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = habit.habitName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${habit.completionRate.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (habit.currentStreak > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = habit.currentStreak.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B35)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenEngagementChart(
    data: List<ScreenVisitChartPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(
            message = "No screen visit data available",
            modifier = modifier
        )
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Bar chart
        val maxVisits = data.maxOfOrNull { it.visitCount } ?: 1
        
        data.forEach { screen ->
            ScreenEngagementBar(
                screen = screen,
                maxVisits = maxVisits,
                progress = animatedProgress.value
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ScreenEngagementBar(
    screen: ScreenVisitChartPoint,
    maxVisits: Int,
    progress: Float
) {
    val barProgress = (screen.visitCount.toFloat() / maxVisits) * progress
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = screen.screenName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${(screen.visitCount * progress).toInt()} visits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
            )
        }
        
        // Engagement score indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Engagement: ${(screen.engagementScore * progress).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    screen.engagementScore >= 80 -> Color(0xFF4CAF50)
                    screen.engagementScore >= 60 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
        }
    }
}

@Composable
fun StreakAnalysisSection(
    data: AnalyticsData
) {
    val activeStreaks = data.streakRetentions.filter { it.isActive }
    val completedStreaks = data.streakRetentions.filter { !it.isActive }
    
    AnalyticsCard(
        title = "Streak Analysis",
        icon = Icons.Default.Timeline,
        subtitle = "${activeStreaks.size} active streaks"
    ) {
        if (activeStreaks.isEmpty() && completedStreaks.isEmpty()) {
            EmptyChartPlaceholder(message = "No streak data available")
            return@AnalyticsCard
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active streaks
            if (activeStreaks.isNotEmpty()) {
                Text(
                    text = "Active Streaks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
                
                activeStreaks.forEach { streak ->
                    StreakItem(
                        streak = streak,
                        isActive = true
                    )
                }
            }
            
            // Completed streaks (top 3)
            if (completedStreaks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Recent Completed Streaks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                completedStreaks.take(3).forEach { streak ->
                    StreakItem(
                        streak = streak,
                        isActive = false
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakItem(
    streak: StreakRetention,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.TrendingUp else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isActive) Color(0xFFFF6B35) else Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = streak.habitName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "${streak.streakLength} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (isActive) {
                Text(
                    text = "${(streak.retentionProbability * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        streak.retentionProbability >= 0.8 -> Color(0xFF4CAF50)
                        streak.retentionProbability >= 0.6 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}

@Composable
fun InsightsSection(
    data: AnalyticsData
) {
    val insights = generateInsights(data)
    
    if (insights.isEmpty()) return
    
    AnalyticsCard(
        title = "Insights & Recommendations",
        icon = Icons.Default.Lightbulb
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            insights.forEach { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: AnalyticsInsight
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.type) {
                InsightType.POSITIVE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                InsightType.IMPROVEMENT -> Color(0xFFFF9800).copy(alpha = 0.1f)
                InsightType.WARNING -> Color(0xFFF44336).copy(alpha = 0.1f)
                InsightType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = insight.icon,
                fontSize = 24.sp,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyChartPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Generate insights based on analytics data
 */
private fun generateInsights(data: AnalyticsData): List<AnalyticsInsight> {
    val insights = mutableListOf<AnalyticsInsight>()
    
    // Completion rate insights
    val avgCompletionRate = data.habitCompletionRates.map { it.completionPercentage }.average()
    when {
        avgCompletionRate >= 80.0 -> insights.add(
            AnalyticsInsight(
                type = InsightType.POSITIVE,
                title = "Excellent Progress!",
                message = "Your average completion rate is ${avgCompletionRate.toInt()}%. Keep up the great work!",
                icon = "🎉"
            )
        )
        avgCompletionRate >= 60.0 -> insights.add(
            AnalyticsInsight(
                type = InsightType.NEUTRAL,
                title = "Good Consistency",
                message = "You're completing ${avgCompletionRate.toInt()}% of your habits. Consider small improvements.",
                icon = "👍"
            )
        )
        else -> insights.add(
            AnalyticsInsight(
                type = InsightType.IMPROVEMENT,
                title = "Room for Growth",
                message = "Consider reducing habit difficulty or frequency to build momentum.",
                icon = "💡"
            )
        )
    }
    
    // Streak insights
    val longestStreak = data.habitCompletionRates.maxOfOrNull { it.longestStreak } ?: 0
    if (longestStreak >= 7) {
        insights.add(
            AnalyticsInsight(
                type = InsightType.POSITIVE,
                title = "Streak Master!",
                message = "Your longest streak is $longestStreak days. Streaks build strong habits!",
                icon = "🔥"
            )
        )
    }
    
    // Engagement insights
    val totalScreenTime = data.screenVisits.sumOf { it.totalTimeSpent }
    if (totalScreenTime > 0) {
        val avgSessionTime = totalScreenTime / data.screenVisits.sumOf { it.visitCount }
        val minutes = avgSessionTime / (1000 * 60)
        
        if (minutes > 5) {
            insights.add(
                AnalyticsInsight(
                    type = InsightType.NEUTRAL,
                    title = "Engaged User",
                    message = "Average session: ${minutes}m. You're actively using the app!",
                    icon = "⏱️"
                )
            )
        }
    }
    
    return insights
}

/**
 * Data class for analytics insights
 */
data class AnalyticsInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val icon: String
)

enum class InsightType {
    POSITIVE, NEUTRAL, IMPROVEMENT, WARNING
}
