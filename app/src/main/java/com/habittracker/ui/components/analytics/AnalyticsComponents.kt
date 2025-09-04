package com.habittracker.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DurationSparkline(
    data: List<Int>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
) {
    // Enhanced defensive checks: validate data before processing
    if (data.isEmpty() || data.size < 2) {
        Box(modifier = modifier.height(48.dp))
        return
    }
    
    // Additional safety checks for data integrity with stricter bounds
    val validData = data.filter { it > 0 && it < 10000 } // Reasonable minutes: 1 min to ~7 days
    if (validData.isEmpty() || validData.size < 2) {
        Box(modifier = modifier.height(48.dp))
        return
    }
    
    // Validate bounds before calculations
    val min = validData.minOrNull()?.coerceAtLeast(0) ?: 0
    val max = validData.maxOrNull()?.coerceAtLeast(1) ?: 1
    val range = (max - min).coerceAtLeast(1)
    
    // Additional validation
    if (min < 0 || max <= 0 || range <= 0 || validData.any { it <= 0 }) {
        Box(modifier = modifier.height(48.dp))
        return
    }

    Canvas(modifier = modifier.height(56.dp)) {
        try {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || validData.isEmpty()) return@Canvas

            val step = if (validData.size <= 1) w else w / (validData.size - 1)

            // Safe point generation with bounds checking
            val points = mutableListOf<Offset>()
            for (i in validData.indices) {
                if (i < 0 || i >= validData.size) continue // Extra safety
                
                val v = validData.getOrNull(i) ?: continue
                if (v <= 0) continue
                
                val x = step * i
                val norm = (v - min).toFloat() / range.toFloat()
                val y = h - (norm * (h * 0.9f)) - (h * 0.05f)
                
                // Validate coordinates
                if (x.isFinite() && y.isFinite() && x >= 0 && y >= 0) {
                    points.add(Offset(x, y))
                }
            }

            if (points.size < 2) return@Canvas

            // Fill area under curve
            val path = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { p -> lineTo(p.x, p.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(path = path, color = fillColor)

            // Stroke with safe iteration
            for (i in 0 until points.lastIndex) {
                if (i >= 0 && i < points.size && i + 1 < points.size) {  // Enhanced bounds check
                    val a = points.getOrNull(i)
                    val b = points.getOrNull(i + 1)
                    if (a != null && b != null) {
                        drawLine(color = lineColor, start = a, end = b, strokeWidth = strokeWidth.toPx())
                    }
                }
            }
        } catch (t: Throwable) {
            // Fail-safe: never crash UI on analytics drawing
            android.util.Log.e("DurationSparkline", "Failed to render sparkline: ${t.message}", t)
        }
    }
}

@Composable
fun AnalyticsSummaryRow(
    averageMinutes: Int?,
    targetMinutes: Int?,
    adherencePercent: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatTile(title = "Avg", value = averageMinutes?.let { "$it min" } ?: "—")
        StatTile(title = "Target", value = targetMinutes?.let { "$it min" } ?: "—")
        StatTile(title = "Adherence", value = adherencePercent?.let { "$it%" } ?: "—")
    }
}

@Composable
private fun StatTile(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
