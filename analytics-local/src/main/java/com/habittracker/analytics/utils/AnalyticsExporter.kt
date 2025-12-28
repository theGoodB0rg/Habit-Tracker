package com.habittracker.analytics.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.*
import com.habittracker.analytics.domain.models.*
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStream
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
        format: ExportFormat,
        isShare: Boolean = false
    ): String {
        try {
            val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val fileName = "analytics_export_${timestamp}.${format.extension}"

            return if (isShare) {
                // Save to cache for sharing
                val cacheDir = File(context.cacheDir, "exports")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val file = File(cacheDir, fileName)
                
                saveToFile(file, analyticsData, format)
                file.absolutePath
            } else {
                // Save to Downloads/Documents for user access
                saveToPublicStorage(context, fileName, analyticsData, format)
            }
        } catch (e: Exception) {
            throw ExportException("Failed to export analytics data: ${e.message}", e)
        }
    }

    private fun saveToFile(file: File, analyticsData: AnalyticsData, format: ExportFormat) {
        when (format) {
            ExportFormat.JSON -> exportToJson(file, analyticsData)
            ExportFormat.CSV -> exportToCsv(file, analyticsData)
            ExportFormat.PDF -> exportToPdf(file, analyticsData)
            ExportFormat.IMAGE -> exportToImage(file, analyticsData)
        }
    }

    private fun saveToPublicStorage(
        context: Context,
        fileName: String,
        analyticsData: AnalyticsData,
        format: ExportFormat
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(format))
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw ExportException("Failed to create file in Downloads")

            resolver.openOutputStream(uri)?.use { outputStream ->
                writeToStream(outputStream, analyticsData, format)
            }
            return uri.toString() // Return URI string for success message
        } else {
            // Legacy storage
            val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HabitTracker")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)
            saveToFile(file, analyticsData, format)
            return file.absolutePath
        }
    }

    private fun writeToStream(outputStream: OutputStream, analyticsData: AnalyticsData, format: ExportFormat) {
        when (format) {
            ExportFormat.JSON -> outputStream.write(gson.toJson(analyticsData).toByteArray())
            ExportFormat.CSV -> {
                val writer = OutputStreamWriter(outputStream)
                writeCsvContent(writer, analyticsData)
            }
            ExportFormat.PDF -> {
                // PDF requires a file usually, but we can adapt or use temp file
                // For simplicity, let's use a temp file and copy
                val tempFile = File.createTempFile("temp_pdf", ".pdf")
                exportToPdf(tempFile, analyticsData)
                tempFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
                tempFile.delete()
            }
            ExportFormat.IMAGE -> {
                val bitmap = createAnalyticsImage(analyticsData)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    }

    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> "application/json"
            ExportFormat.CSV -> "text/csv"
            ExportFormat.PDF -> "application/pdf"
            ExportFormat.IMAGE -> "image/png"
        }
    }

    /**
     * Export to JSON format
     */
    private fun exportToJson(file: File, analyticsData: AnalyticsData) {
        FileOutputStream(file).use { fos ->
            fos.write(gson.toJson(analyticsData).toByteArray())
        }
    }

    /**
     * Export to CSV format
     */
    private fun exportToCsv(file: File, analyticsData: AnalyticsData) {
        FileWriter(file).use { writer ->
            writeCsvContent(writer, analyticsData)
        }
    }

    private fun writeCsvContent(writer: OutputStreamWriter, analyticsData: AnalyticsData) {
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

    /**
     * Export to Image format (Viral Story Style)
     */
    private fun exportToImage(file: File, analyticsData: AnalyticsData) {
        val bitmap = createAnalyticsImage(analyticsData)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    private fun createAnalyticsImage(analyticsData: AnalyticsData): Bitmap {
        val width = 1080
        val height = 1920 // 9:16 aspect ratio
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Vibrant Background Gradient (Deep Purple to Blue/Pink)
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.parseColor("#4A00E0"), Color.parseColor("#8E2DE2")), 
            null,
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Glassmorphism Card
        val cardPadding = 80f
        val cardTop = 250f
        val cardBottom = height - 250f
        val cardRect = RectF(cardPadding, cardTop, width - cardPadding, cardBottom)
        val cardPaint = Paint().apply {
            color = Color.WHITE
            alpha = 245 // High opacity for readability but still soft
            style = Paint.Style.FILL
            setShadowLayer(60f, 0f, 30f, Color.parseColor("#80000000"))
        }
        canvas.drawRoundRect(cardRect, 60f, 60f, cardPaint)

        // 3. Header
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 90f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(20f, 0f, 10f, Color.parseColor("#40000000"))
        }
        canvas.drawText("My Monthly Recap", width / 2f, 180f, titlePaint)

        // 4. Hero Section: Circular Progress
        val overallRate = if (analyticsData.habitCompletionRates.isNotEmpty()) {
            analyticsData.habitCompletionRates.map { it.completionPercentage }.average()
        } else 0.0

        val centerX = width / 2f
        val centerY = cardTop + 350f
        val radius = 220f
        val ringStrokeWidth = 50f

        // Track background
        val trackPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = ringStrokeWidth
            color = Color.parseColor("#F0F0F0")
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, trackPaint)

        // Progress arc
        val progressPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = ringStrokeWidth
            color = Color.parseColor("#8E2DE2") // Match gradient theme
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val arcRect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(arcRect, -90f, (3.6f * overallRate).toFloat(), false, progressPaint)

        // Percentage Text
        val percentPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 140f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        // Center text vertically
        val textBounds = Rect()
        val percentText = "${overallRate.toInt()}%"
        percentPaint.getTextBounds(percentText, 0, percentText.length, textBounds)
        val textHeight = textBounds.height()
        canvas.drawText(percentText, centerX, centerY + (textHeight / 2f) - 10f, percentPaint)
        
        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Consistency", centerX, centerY + radius + 80f, labelPaint)

        // 5. Stats Row
        val maxStreak = analyticsData.habitCompletionRates.maxOfOrNull { it.longestStreak } ?: 0
        val totalHabits = analyticsData.habitCompletionRates.size
        
        val statLabelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val statValuePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val rowY = centerY + radius + 200f
        // Left Stat: Longest Streak
        canvas.drawText("Longest Streak", centerX - 200f, rowY, statLabelPaint)
        canvas.drawText("$maxStreak Days \uD83D\uDD25", centerX - 200f, rowY + 70f, statValuePaint)

        // Right Stat: Active Habits
        canvas.drawText("Active Habits", centerX + 200f, rowY, statLabelPaint)
        canvas.drawText("$totalHabits", centerX + 200f, rowY + 70f, statValuePaint)

        // 6. Top Habits List
        val listStartY = rowY + 180f
        val topHabits = analyticsData.habitCompletionRates.sortedByDescending { it.completionPercentage }.take(3)
        
        val habitNamePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        val habitStatPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 36f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        var currentY = listStartY
        val listPadding = 150f
        val maxTextWidth = width - (listPadding * 2) - 200f // Reserve space for progress bar

        topHabits.forEach { habit ->
            // Truncate name if too long
            val name = if (habitNamePaint.measureText(habit.habitName) > maxTextWidth) {
                var truncated = habit.habitName
                while (habitNamePaint.measureText(truncated + "...") > maxTextWidth && truncated.isNotEmpty()) {
                    truncated = truncated.dropLast(1)
                }
                "$truncated..."
            } else {
                habit.habitName
            }

            canvas.drawText(name, listPadding, currentY, habitNamePaint)
            canvas.drawText("${habit.completionPercentage.toInt()}% completed", listPadding, currentY + 50f, habitStatPaint)
            
            // Mini progress bar
            val barWidth = 200f
            val barHeight = 20f
            val barX = width - listPadding - barWidth
            val barY = currentY - 20f
            
            // Background bar
            val barBgPaint = Paint().apply { color = Color.parseColor("#EEEEEE"); style = Paint.Style.FILL }
            canvas.drawRoundRect(barX, barY, barX + barWidth, barY + barHeight, 10f, 10f, barBgPaint)
            
            // Foreground bar
            val barFgPaint = Paint().apply { color = Color.parseColor("#8E2DE2"); style = Paint.Style.FILL }
            val progressWidth = (habit.completionPercentage / 100.0 * barWidth).toFloat()
            canvas.drawRoundRect(barX, barY, barX + progressWidth, barY + barHeight, 10f, 10f, barFgPaint)

            currentY += 140f
        }

        // 7. Footer
        val footerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            alpha = 200
        }
        canvas.drawText("Tracked with Offline Habit Tracker", width / 2f, height - 120f, footerPaint)

        return bitmap
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
