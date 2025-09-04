package com.habittracker.analytics.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.analytics.domain.models.ExportFormat
import com.habittracker.analytics.domain.usecases.ExportAnalyticsUseCase
import kotlinx.coroutines.launch

@Composable
fun ExportButton(
    exportAnalyticsUseCase: ExportAnalyticsUseCase,
    format: ExportFormat = ExportFormat.JSON,
    onExportComplete: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            coroutineScope.launch {
                isExporting = true
                try {
                    val result = exportAnalyticsUseCase(format)
                    onExportComplete(result)
                } catch (e: Exception) {
                    onExportComplete("Export failed: ${e.message}")
                } finally {
                    isExporting = false
                }
            }
        },
        enabled = !isExporting,
        colors = ButtonDefaults.buttonColors(),
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = if (isExporting) "Exporting..." else "Export Analytics",
            fontSize = 16.sp
        )
    }
}