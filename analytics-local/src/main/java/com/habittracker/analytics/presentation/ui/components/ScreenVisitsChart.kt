package com.habittracker.analytics.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun ScreenVisitsChart(screenVisits: List<Int>, modifier: Modifier = Modifier) {
    val maxVisits = screenVisits.maxOrNull() ?: 1
    val chartHeight = 200.dp
    val chartWidth = 300.dp

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Screen Visits",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(modifier = Modifier.size(chartWidth, chartHeight)) {
            drawChart(screenVisits, maxVisits)
        }
    }
}

private fun DrawScope.drawChart(screenVisits: List<Int>, maxVisits: Int) {
    val widthPerVisit = size.width / screenVisits.size
    val path = Path()

    screenVisits.forEachIndexed { index, visits ->
        val x = index * widthPerVisit
        val y = size.height - (visits.toFloat() / maxVisits * size.height)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = Color.Blue,
        style = Stroke(width = 4f)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewScreenVisitsChart() {
    ScreenVisitsChart(screenVisits = listOf(5, 10, 15, 20, 25))
}