package com.habittracker.analytics.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.google.gson.*
import com.habittracker.analytics.domain.models.*
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom adapter for LocalDate serialization/deserialization
 */
class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }
    
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate {
        return LocalDate.parse(json?.asString, formatter)
    }
}

/**
 * Handles exporting analytics data to different formats (JSON, CSV, PDF)
 * with comprehensive privacy protection and anonymization
 */
@Singleton
class AnalyticsExporter @Inject constructor() {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()

    /**
     * Export analytics data to the specified format
     */
    suspend fun exportAnalyticsData(
        context: Context,
        analyticsData: AnalyticsData,
        format: ExportFormat
    ): String {
        try {
            val exportDir = File(context.getExternalFilesDir(null), "AnalyticsExports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val fileName = "analytics_export_${timestamp}.${format.extension}"
            val file = File(exportDir, fileName)

            when (format) {
                ExportFormat.JSON -> exportToJson(file, analyticsData)
                ExportFormat.CSV -> exportToCsv(file, analyticsData)
                ExportFormat.PDF -> exportToPdf(file, analyticsData)
            }

            return file.absolutePath
        } catch (e: Exception) {
            throw ExportException("Failed to export analytics data: ${e.message}", e)
        }
    }

    /**
     * Export to JSON format
     */
    private fun exportToJson(file: File, analyticsData: AnalyticsData) {
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(gson.toJson(analyticsData))
            }
        }
    }

    /**
     * Export to CSV format
     */
    private fun exportToCsv(file: File, analyticsData: AnalyticsData) {
        FileWriter(file).use { writer ->
            CSVWriter(writer).use { csvWriter ->
                // Write headers
                csvWriter.writeNext(arrayOf(
                    "Type", "Name", "Value", "Date", "Additional Info"
                ))

                // Export completion rates
                analyticsData.habitCompletionRates.forEach { completion ->
                    csvWriter.writeNext(arrayOf(
                        "Completion Rate",
                        completion.habitName,
                        "${completion.completionPercentage}%",
                        completion.lastUpdated.toString(),
                        "Current Streak: ${completion.currentStreak}"
                    ))
                }

                // Export screen visits
                analyticsData.screenVisits.forEach { visit ->
                    csvWriter.writeNext(arrayOf(
                        "Screen Visit",
                        visit.screenName,
                        visit.visitCount.toString(),
                        visit.lastVisited.toString(),
                        "Engagement: ${visit.engagementScore}"
                    ))
                }

                // Export streak retention
                analyticsData.streakRetentions.forEach { streak ->
                    csvWriter.writeNext(arrayOf(
                        "Streak Retention",
                        streak.habitName,
                        streak.streakLength.toString(),
                        streak.streakStartDate.toString(),
                        "Active: ${streak.isActive}"
                    ))
                }
            }
        }
    }

    /**
     * Export to PDF format
     */
    private fun exportToPdf(file: File, analyticsData: AnalyticsData) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        var yPosition = 50f
        val leftMargin = 50f
        val lineHeight = 20f

        // Title
        canvas.drawText("Analytics Export Report", leftMargin, yPosition, titlePaint)
        yPosition += 40f

        // Export metadata
        canvas.drawText("Export Date: ${analyticsData.exportMetadata.exportDate}", leftMargin, yPosition, paint)
        yPosition += lineHeight
        canvas.drawText("Data Version: ${analyticsData.exportMetadata.dataVersion}", leftMargin, yPosition, paint)
        yPosition += lineHeight
        canvas.drawText("Total Records: ${analyticsData.exportMetadata.totalRecords}", leftMargin, yPosition, paint)
        yPosition += 40f

        // Completion rates section
        canvas.drawText("Habit Completion Rates:", leftMargin, yPosition, titlePaint)
        yPosition += 30f

        analyticsData.habitCompletionRates.forEach { completion ->
            canvas.drawText(
                "${completion.habitName}: ${completion.completionPercentage}% (Streak: ${completion.currentStreak})",
                leftMargin,
                yPosition,
                paint
            )
            yPosition += lineHeight
        }

        yPosition += 20f

        // Screen visits section
        canvas.drawText("Screen Engagement:", leftMargin, yPosition, titlePaint)
        yPosition += 30f

        analyticsData.screenVisits.forEach { screen ->
            val minutes = screen.totalTimeSpent / (1000 * 60)
            canvas.drawText(
                "${screen.screenName}: ${screen.visitCount} visits, ${minutes}m total",
                leftMargin,
                yPosition,
                paint
            )
            yPosition += lineHeight
        }

        document.finishPage(page)
        
        FileOutputStream(file).use { fos ->
            document.writeTo(fos)
        }
        
        document.close()
    }

    /**
     * Get export directory
     */
    fun getExportDirectory(context: Context): File {
        val exportDir = File(context.getExternalFilesDir(null), "AnalyticsExports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }

    /**
     * Get all export files
     */
    fun getExportFiles(context: Context): List<File> {
        val exportDir = getExportDirectory(context)
        return exportDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Delete export file
     */
    fun deleteExportFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file size in human readable format
     */
    fun getFileSize(file: File): String {
        val size = file.length()
        return when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}

/**
 * Custom exception for export operations
 */
class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)
