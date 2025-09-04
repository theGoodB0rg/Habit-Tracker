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
import androidx.compose.ui.unit.sp
import com.habittracker.analytics.domain.models.CompletionRate
import com.habittracker.analytics.domain.models.TimeFrame
import java.time.LocalDate

@Composable
fun CompletionRateChart(
    completionRate: CompletionRate,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Completion Rate",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Placeholder for the chart visualization
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${completionRate.completionPercentage.toInt()}%",
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Completed: ${completionRate.completedDays} / Total: ${completionRate.totalDays}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompletionRateChartPreview() {
    CompletionRateChart(
        completionRate = CompletionRate(
            habitId = "habit1",
            habitName = "Morning Exercise",
            totalDays = 10,
            completedDays = 7,
            completionPercentage = 70.0,
            currentStreak = 3,
            longestStreak = 5,
            weeklyAverage = 4.5,
            monthlyAverage = 18.0,
            timeFrame = TimeFrame.WEEKLY,
            lastUpdated = LocalDate.now()
        )
    )
}