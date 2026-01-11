package com.habittracker.features.smart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 🧠 SMART HABIT INSIGHTS - The Game Changer
 * 
 * This transforms the app from a basic tracker to an intelligent habit coach
 * Features that provide REAL value:
 */

/**
 * 1. INTELLIGENT TIMING OPTIMIZATION
 * Analyzes when user is most likely to complete habits successfully
 */
@Composable
fun SmartTimingInsights(
    habitId: Long,
    modifier: Modifier = Modifier
) {
    // AI-powered analysis of optimal habit timing
    val insights = remember {
        SmartTimingAnalyzer.analyzeOptimalTiming(habitId)
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Timing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Personalized insights
            Text(
                text = "You're ${insights.successRate}% more likely to succeed when you do this habit at ${insights.optimalTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Your best days: ${insights.bestDays.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 2. HABIT STACK RECOMMENDATIONS
 * AI suggests habit combinations that work well together
 */
@Composable
fun HabitStackSuggestions(
    currentHabit: Long,
    modifier: Modifier = Modifier
) {
    val suggestions = remember {
        HabitStackAnalyzer.getSuggestions(currentHabit)
    }
    
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Stacking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "People who do this habit also succeed with:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            LazyColumn(
                modifier = Modifier.height(120.dp)
            ) {
                items(suggestions) { suggestion ->
                    HabitStackItem(suggestion = suggestion)
                }
            }
        }
    }
}

/**
 * 3. PREDICTIVE FAILURE PREVENTION
 * AI predicts when user might fail and suggests interventions
 */
@Composable
fun FailurePrevention(
    modifier: Modifier = Modifier
) {
    val riskAnalysis = remember {
        FailurePredictor.analyzeRisk()
    }
    
    if (riskAnalysis.isHighRisk) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Risk Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = riskAnalysis.warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { /* Apply suggested intervention */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Apply Recovery Plan")
                }
            }
        }
    }
}

/**
 * 4. SOCIAL ACCOUNTABILITY FEATURES
 * Connect with friends/family for motivation
 */
@Composable
fun SocialAccountability(
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accountability",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show accountability partner status
            Text(
                text = "Sarah is on a 12-day streak with 'Morning Workout'",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "You're both crushing it!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                Button(
                    onClick = { /* Send encouragement */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Row( verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                       Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                       Spacer(Modifier.width(8.dp))
                       Text("Encourage")
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedButton(
                    onClick = { /* Challenge friend */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Challenge")
                }
            }
        }
    }
}

/**
 * 5. ENVIRONMENTAL CONTEXT AWARENESS
 * Adapts suggestions based on location, weather, calendar
 */
@Composable
fun ContextualInsights(
    modifier: Modifier = Modifier
) {
    val context = remember {
        ContextAnalyzer.getCurrentContext()
    }
    
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (context.type) {
                ContextType.RAINY_DAY -> {
                    LabelWithIcon(Icons.Default.WaterDrop, "Perfect day for indoor habits! Try meditation or reading.")
                }
                ContextType.FREE_TIME -> {
                    LabelWithIcon(Icons.Default.AccessTime, "You have 30 min free. Great time for your exercise habit!")
                }
                ContextType.STRESSFUL_DAY -> {
                    LabelWithIcon(Icons.Default.SelfImprovement, "Detected high stress. Consider skipping intense habits today.")
                }
                ContextType.TRAVEL -> {
                    LabelWithIcon(Icons.Default.Flight, "Traveling? Here are habit modifications for your trip.")
                }
            }
            }
        }
    }


// Data classes for the smart features
data class TimingInsight(
    val optimalTime: LocalTime,
    val successRate: Int,
    val bestDays: List<String>
)

data class HabitSuggestion(
    val name: String,
    val compatibility: Float,
    val reason: String
)

data class RiskAnalysis(
    val isHighRisk: Boolean,
    val warning: String,
    val intervention: String
)

enum class ContextType {
    RAINY_DAY, FREE_TIME, STRESSFUL_DAY, TRAVEL
}

data class EnvironmentalContext(
    val type: ContextType,
    val confidence: Float
)

// Mock analyzer objects (would be implemented with real ML/AI)
object SmartTimingAnalyzer {
    fun analyzeOptimalTiming(habitId: Long): TimingInsight {
        return TimingInsight(
            optimalTime = LocalTime.of(7, 30),
            successRate = 83,
            bestDays = listOf("Monday", "Wednesday", "Friday")
        )
    }
}

object HabitStackAnalyzer {
    fun getSuggestions(habitId: Long): List<HabitSuggestion> {
        return listOf(
            HabitSuggestion("Meditation", 0.9f, "Enhances focus for other habits"),
            HabitSuggestion("Hydration", 0.8f, "Pairs well with morning routines")
        )
    }
}

object FailurePredictor {
    fun analyzeRisk(): RiskAnalysis {
        return RiskAnalysis(
            isHighRisk = true,
            warning = "You've missed 2 days in a row. Pattern suggests risk of abandoning this habit.",
            intervention = "Try reducing the habit difficulty by 50% for this week."
        )
    }
}

object ContextAnalyzer {
    fun getCurrentContext(): EnvironmentalContext {
        return EnvironmentalContext(
            type = ContextType.RAINY_DAY,
            confidence = 0.8f
        )
    }
}

@Composable
fun HabitStackItem(suggestion: HabitSuggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = suggestion.name,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(suggestion.compatibility * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
private fun LabelWithIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
