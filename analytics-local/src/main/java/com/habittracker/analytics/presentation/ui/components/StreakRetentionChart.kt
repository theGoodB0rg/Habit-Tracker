package com.habittracker.analytics.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.habittracker.analytics.domain.models.StreakRetention
import com.habittracker.analytics.domain.models.DifficultyLevel
import com.habittracker.analytics.domain.models.TimeFrame
import java.time.LocalDate

@Composable
fun StreakRetentionChart(
    streakRetentionData: List<StreakRetention>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Streak Retention",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Placeholder for the chart visualization
        // Replace with actual chart implementation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Chart goes here", color = Color.DarkGray)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display streak retention data
        streakRetentionData.forEach { streak ->
            Text(
                text = "${streak.habitName}: ${streak.streakLength} days (${if (streak.isActive) "Active" else "Inactive"})",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StreakRetentionChartPreview() {
    StreakRetentionChart(
        streakRetentionData = listOf(
            StreakRetention(
                habitId = "habit1",
                habitName = "Morning Exercise",
                streakLength = 5,
                streakStartDate = LocalDate.now().minusDays(5),
                streakEndDate = null,
                isActive = true,
                retentionProbability = 0.85,
                difficultyLevel = DifficultyLevel.MODERATE,
                timeFrame = TimeFrame.WEEKLY
            ),
            StreakRetention(
                habitId = "habit2",
                habitName = "Reading",
                streakLength = 3,
                streakStartDate = LocalDate.now().minusDays(3),
                streakEndDate = LocalDate.now().minusDays(1),
                isActive = false,
                retentionProbability = 0.65,
                difficultyLevel = DifficultyLevel.EASY,
                timeFrame = TimeFrame.WEEKLY
            )
        )
    )
}