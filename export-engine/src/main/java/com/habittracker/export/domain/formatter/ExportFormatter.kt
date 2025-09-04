package com.habittracker.export.domain.formatter

import com.google.gson.GsonBuilder
import com.habittracker.export.data.model.HabitCsvRow
import com.habittracker.export.data.model.HabitExportData
import com.habittracker.export.data.model.PngExportData
import com.habittracker.export.domain.exception.SerializationException
import com.habittracker.export.domain.renderer.PngExportRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formats export data into different file formats
 */
interface ExportFormatter {
    suspend fun formatAsJson(data: HabitExportData): String
    suspend fun formatAsCsv(rows: List<HabitCsvRow>): String
    suspend fun formatAsPng(data: PngExportData): ByteArray
}

/**
 * Implementation of ExportFormatter with proper error handling and performance optimization
 */
@Singleton
class ExportFormatterImpl @Inject constructor(
    private val pngRenderer: PngExportRenderer
) : ExportFormatter {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    override suspend fun formatAsJson(data: HabitExportData): String = withContext(Dispatchers.IO) {
        try {
            gson.toJson(data)
        } catch (e: Exception) {
            throw SerializationException(
                "Failed to serialize data to JSON",
                e
            )
        }
    }

    override suspend fun formatAsCsv(rows: List<HabitCsvRow>): String = withContext(Dispatchers.IO) {
        try {
            val csvBuilder = StringBuilder()
            
            // Add CSV header
            csvBuilder.appendLine(getCsvHeader())
            
            // Add data rows
            rows.forEach { row ->
                csvBuilder.appendLine(formatCsvRow(row))
            }
            
            csvBuilder.toString()
        } catch (e: Exception) {
            throw SerializationException(
                "Failed to serialize data to CSV",
                e
            )
        }
    }

    override suspend fun formatAsPng(data: PngExportData): ByteArray = withContext(Dispatchers.IO) {
        try {
            pngRenderer.renderToPng(data)
        } catch (e: Exception) {
            throw SerializationException(
                "Failed to render data to PNG",
                e
            )
        }
    }

    private fun getCsvHeader(): String {
        return listOf(
            "Habit ID",
            "Habit Name",
            "Description",
            "Frequency",
            "Created Date",
            "Current Streak",
            "Longest Streak",
            "Last Completed Date",
            "Is Active",
            "Completion Date",
            "Completion Time",
            "Completion Note"
        ).joinToString(",") { escapeField(it) }
    }

    private fun formatCsvRow(row: HabitCsvRow): String {
        return listOf(
            row.habitId.toString(),
            row.habitName,
            row.description,
            row.frequency,
            row.createdDate,
            row.streakCount.toString(),
            row.longestStreak.toString(),
            row.lastCompletedDate ?: "",
            row.isActive.toString(),
            row.completionDate ?: "",
            row.completionTime ?: "",
            row.completionNote ?: ""
        ).joinToString(",") { escapeField(it) }
    }

    /**
     * Escape CSV field by wrapping in quotes and escaping internal quotes
     */
    private fun escapeField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\"" 
        } else {
            field
        }
    }
}
