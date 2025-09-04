package com.habittracker.export.domain.renderer

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.habittracker.export.data.model.*
import com.habittracker.export.domain.exception.RenderingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Professional Canvas-based PNG renderer with memory management and race condition protection
 * Implements Google Material Design 3 principles with accessibility support
 */
@Singleton
class PngExportRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val BITMAP_QUALITY = 100
        private const val DEFAULT_DPI = 320 // High DPI for crisp sharing
        private const val MAX_BITMAP_MEMORY = 50 * 1024 * 1024 // 50MB memory limit
        private const val CORNER_RADIUS = 12f
        private const val SHADOW_RADIUS = 8f
        
        // Typography scale based on Material 3
        private const val DISPLAY_LARGE = 57f
        private const val DISPLAY_MEDIUM = 45f
        private const val DISPLAY_SMALL = 36f
        private const val HEADLINE_LARGE = 32f
        private const val HEADLINE_MEDIUM = 28f
        private const val HEADLINE_SMALL = 24f
        private const val TITLE_LARGE = 22f
        private const val TITLE_MEDIUM = 16f
        private const val TITLE_SMALL = 14f
        private const val BODY_LARGE = 16f
        private const val BODY_MEDIUM = 14f
        private const val BODY_SMALL = 12f
        private const val LABEL_LARGE = 14f
        private const val LABEL_MEDIUM = 12f
        private const val LABEL_SMALL = 11f
    }
    
    private var currentBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paintPool = mutableListOf<Paint>()
    private val pathPool = mutableListOf<Path>()
    
    // Thread-safe rendering with mutex
    private val renderMutex = kotlinx.coroutines.sync.Mutex()
    
    /**
     * Main entry point for PNG rendering with comprehensive error handling
     */
    suspend fun renderToPng(data: PngExportData): ByteArray = withContext(Dispatchers.Default) {
        renderMutex.lock()
        try {
            validateInputData(data)
            
            val layoutConfig = calculateLayoutConfig(data)
            initializeBitmap(layoutConfig)
            
            renderAllSections(data, layoutConfig)
            
            return@withContext compressToPng()
        } catch (e: OutOfMemoryError) {
            throw RenderingException("Insufficient memory for PNG generation", e)
        } catch (e: Exception) {
            throw RenderingException("Failed to render PNG: ${e.message}", e)
        } finally {
            cleanup()
            renderMutex.unlock()
        }
    }
    
    /**
     * Validates input data to prevent rendering errors
     */
    private fun validateInputData(data: PngExportData) {
        require(data.metadata.totalHabits >= 0) { "Total habits cannot be negative" }
        require(data.statistics.completionRate in 0.0..1.0) { "Completion rate must be between 0 and 1" }
        require(data.heatmapData.cells.isNotEmpty()) { "Heatmap data cannot be empty" }
        
        // Validate memory requirements
        val aspectRatio = data.customization.aspectRatio
        val requiredMemory = aspectRatio.width * aspectRatio.height * 4 // ARGB bytes
        require(requiredMemory <= MAX_BITMAP_MEMORY) { 
            "Requested bitmap size exceeds memory limit" 
        }
    }
    
    /**
     * Calculates optimal layout configuration based on data and customization
     */
    private fun calculateLayoutConfig(data: PngExportData): PngLayoutConfig {
        val aspectRatio = data.customization.aspectRatio
        val theme = data.customization.theme
        
        // Calculate responsive padding based on canvas size
        val basePadding = (aspectRatio.width * 0.022f).toInt() // ~2.2% of width
        val padding = PaddingConfig(
            outer = basePadding,
            section = (basePadding * 0.67f).toInt(),
            element = (basePadding * 0.33f).toInt(),
            text = (basePadding * 0.17f).toInt()
        )
        
        // Define responsive sections with weights
        val sections = listOf(
            LayoutSection(SectionType.HEADER, 0.12f, 80),
            LayoutSection(SectionType.STATISTICS, 0.25f, 200),
            LayoutSection(SectionType.HEATMAP, 0.35f, 300),
            LayoutSection(SectionType.ACHIEVEMENTS, 0.18f, 120),
            LayoutSection(SectionType.FOOTER, 0.10f, 60)
        )
        
        val typography = TypographyConfig(
            titleSize = HEADLINE_LARGE * data.customization.fontScale,
            headingSize = TITLE_LARGE * data.customization.fontScale,
            bodySize = BODY_LARGE * data.customization.fontScale,
            captionSize = LABEL_MEDIUM * data.customization.fontScale
        )
        
        val colorPalette = getColorPalette(theme, data.customization.colorScheme)
        
        return PngLayoutConfig(
            canvasWidth = aspectRatio.width,
            canvasHeight = aspectRatio.height,
            padding = padding,
            sections = sections,
            typography = typography,
            colorPalette = colorPalette
        )
    }
    
    /**
     * Initializes bitmap with proper memory management
     */
    private fun initializeBitmap(config: PngLayoutConfig) {
        try {
            currentBitmap = Bitmap.createBitmap(
                config.canvasWidth,
                config.canvasHeight,
                Bitmap.Config.ARGB_8888
            ).also { bitmap ->
                canvas = Canvas(bitmap).apply {
                    // Fill with background color
                    drawColor(config.colorPalette.background)
                }
            }
        } catch (e: OutOfMemoryError) {
            System.gc() // Force garbage collection
            throw RenderingException("Cannot allocate bitmap memory", e)
        }
    }
    
    /**
     * Renders all sections sequentially with proper error handling
     */
    private suspend fun renderAllSections(data: PngExportData, config: PngLayoutConfig) {
        var currentY = config.padding.outer.toFloat()
        
        for (section in config.sections) {
            val sectionHeight = calculateSectionHeight(section, config, currentY)
            
            when (section.type) {
                SectionType.HEADER -> renderHeaderSection(data, config, currentY, sectionHeight)
                SectionType.STATISTICS -> renderStatisticsSection(data, config, currentY, sectionHeight)
                SectionType.HEATMAP -> renderHeatmapSection(data, config, currentY, sectionHeight)
                SectionType.ACHIEVEMENTS -> renderAchievementsSection(data, config, currentY, sectionHeight)
                SectionType.FOOTER -> renderFooterSection(data, config, currentY, sectionHeight)
                else -> { /* Future section types */ }
            }
            
            currentY += sectionHeight + config.padding.section
        }
    }
    
    /**
     * Calculates actual section height based on available space
     */
    private fun calculateSectionHeight(
        section: LayoutSection,
        config: PngLayoutConfig,
        currentY: Float
    ): Float {
        val availableHeight = config.canvasHeight - currentY - config.padding.outer
        val weightedHeight = availableHeight * section.weight
        
        return maxOf(
            section.minHeight.toFloat(),
            minOf(weightedHeight, section.maxHeight?.toFloat() ?: Float.MAX_VALUE)
        )
    }
    
    /**
     * Renders header section with app branding and title
     */
    private fun renderHeaderSection(
        data: PngExportData,
        config: PngLayoutConfig,
        y: Float,
        height: Float
    ) {
        val canvas = this.canvas ?: return
        val padding = config.padding
        
        // Background with subtle gradient
        val headerRect = RectF(
            padding.outer.toFloat(),
            y,
            config.canvasWidth - padding.outer.toFloat(),
            y + height
        )
        
        renderCardBackground(canvas, headerRect, config.colorPalette.surface)
        
        // App logo (if visible)
        var currentX = headerRect.left + padding.element
        if (data.customization.logoVisibility != LogoVisibility.HIDDEN) {
            currentX += renderAppLogo(canvas, currentX, y + height / 2, height * 0.6f)
            currentX += padding.element
        }
        
        // Title text
        val titlePaint = getPaint().apply {
            color = config.colorPalette.onSurface
            textSize = config.typography.titleSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val title = "My Habit Journey"
        val titleY = y + height / 2 + titlePaint.textSize / 3
        canvas.drawText(title, currentX, titleY, titlePaint)
        
        // Date range subtitle
        val subtitlePaint = getPaint().apply {
            color = adjustAlpha(config.colorPalette.onSurface, 0.7f)
            textSize = config.typography.bodySize
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        
        val dateRange = formatDateRange(data.metadata.timeRange)
        val subtitleY = titleY + config.typography.bodySize + padding.text
        canvas.drawText(dateRange, currentX, subtitleY, subtitlePaint)
        
        recyclePaint(titlePaint)
        recyclePaint(subtitlePaint)
    }
    
    /**
     * Renders statistics section with key metrics
     */
    private fun renderStatisticsSection(
        data: PngExportData,
        config: PngLayoutConfig,
        y: Float,
        height: Float
    ) {
        val canvas = this.canvas ?: return
        val padding = config.padding
        val stats = data.statistics
        
        val sectionRect = RectF(
            padding.outer.toFloat(),
            y,
            config.canvasWidth - padding.outer.toFloat(),
            y + height
        )
        
        renderCardBackground(canvas, sectionRect, config.colorPalette.surface)
        
        // Create grid layout for statistics
        val gridColumns = 2
        val gridRows = 2
        val cellWidth = (sectionRect.width() - padding.element * (gridColumns + 1)) / gridColumns
        val cellHeight = (sectionRect.height() - padding.element * (gridRows + 1)) / gridRows
        
        val statisticItems = listOf(
            StatisticItem("Total Completions", stats.totalCompletions.toString(), config.colorPalette.primary),
            StatisticItem("Current Streak", "${stats.currentStreak} days", config.colorPalette.success),
            StatisticItem("Completion Rate", "${(stats.completionRate * 100).toInt()}%", config.colorPalette.secondary),
            StatisticItem("Longest Streak", "${stats.longestStreak} days", config.colorPalette.warning)
        )
        
        statisticItems.forEachIndexed { index, item ->
            val col = index % gridColumns
            val row = index / gridColumns
            
            val cellX = sectionRect.left + padding.element + col * (cellWidth + padding.element)
            val cellY = sectionRect.top + padding.element + row * (cellHeight + padding.element)
            
            renderStatisticCard(canvas, config, cellX, cellY, cellWidth, cellHeight, item)
        }
    }
    
    /**
     * Renders individual statistic card
     */
    private fun renderStatisticCard(
        canvas: Canvas,
        config: PngLayoutConfig,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        item: StatisticItem
    ) {
        val cardRect = RectF(x, y, x + width, y + height)
        
        // Card background with accent color
        val cardPaint = getPaint().apply {
            color = adjustAlpha(item.accentColor, 0.1f)
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, cardPaint)
        
        // Value text (large, prominent)
        val valuePaint = getPaint().apply {
            color = item.accentColor
            textSize = config.typography.headingSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = cardRect.centerX()
        val centerY = cardRect.centerY()
        
        canvas.drawText(item.value, centerX, centerY - config.padding.text, valuePaint)
        
        // Label text (smaller, below value)
        val labelPaint = getPaint().apply {
            color = config.colorPalette.onSurface
            textSize = config.typography.captionSize
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText(item.label, centerX, centerY + config.typography.captionSize, labelPaint)
        
        recyclePaint(cardPaint)
        recyclePaint(valuePaint)
        recyclePaint(labelPaint)
    }
    
    /**
     * Renders heatmap section with GitHub-style contribution visualization
     */
    private fun renderHeatmapSection(
        data: PngExportData,
        config: PngLayoutConfig,
        y: Float,
        height: Float
    ) {
        val canvas = this.canvas ?: return
        val padding = config.padding
        val heatmapData = data.heatmapData
        
        val sectionRect = RectF(
            padding.outer.toFloat(),
            y,
            config.canvasWidth - padding.outer.toFloat(),
            y + height
        )
        
        renderCardBackground(canvas, sectionRect, config.colorPalette.surface)
        
        // Heatmap title
        val titlePaint = getPaint().apply {
            color = config.colorPalette.onSurface
            textSize = config.typography.headingSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val titleY = sectionRect.top + padding.element + config.typography.headingSize
        canvas.drawText("Activity Heatmap", sectionRect.left + padding.element, titleY, titlePaint)
        
        // Calculate heatmap grid dimensions
        val availableWidth = sectionRect.width() - 2 * padding.element
        val availableHeight = sectionRect.height() - titleY - padding.element * 2
        
        val weeksCount = heatmapData.cells.size / 7 + 1
        val cellSize = minOf(
            availableWidth / weeksCount,
            availableHeight / 7
        ) * 0.9f // 90% to allow spacing
        
        val startX = sectionRect.left + padding.element
        val startY = titleY + padding.element
        
        // Render heatmap cells
        heatmapData.cells.forEachIndexed { index, cell ->
            val week = index / 7
            val dayOfWeek = index % 7
            
            val cellX = startX + week * cellSize
            val cellY = startY + dayOfWeek * cellSize
            
            renderHeatmapCell(canvas, cellX, cellY, cellSize * 0.8f, cell, config)
        }
        
        // Render legend
        renderHeatmapLegend(canvas, config, sectionRect, heatmapData.legend)
        
        recyclePaint(titlePaint)
    }
    
    /**
     * Renders individual heatmap cell
     */
    private fun renderHeatmapCell(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        cell: HeatmapCell,
        config: PngLayoutConfig
    ) {
        val cellPaint = getPaint().apply {
            color = interpolateHeatmapColor(cell.intensity, config.colorPalette.heatmapColors)
            isAntiAlias = true
        }
        
        val cellRect = RectF(x, y, x + size, y + size)
        canvas.drawRoundRect(cellRect, size * 0.1f, size * 0.1f, cellPaint)
        
        recyclePaint(cellPaint)
    }
    
    /**
     * Renders achievements section with badges and milestones
     */
    private fun renderAchievementsSection(
        data: PngExportData,
        config: PngLayoutConfig,
        y: Float,
        height: Float
    ) {
        val canvas = this.canvas ?: return
        val padding = config.padding
        
        val sectionRect = RectF(
            padding.outer.toFloat(),
            y,
            config.canvasWidth - padding.outer.toFloat(),
            y + height
        )
        
        renderCardBackground(canvas, sectionRect, config.colorPalette.surface)
        
        // Section title
        val titlePaint = getPaint().apply {
            color = config.colorPalette.onSurface
            textSize = config.typography.headingSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val titleY = sectionRect.top + padding.element + config.typography.headingSize
        canvas.drawText("Recent Achievements", sectionRect.left + padding.element, titleY, titlePaint)
        
        // Render achievement badges
        val availableWidth = sectionRect.width() - 2 * padding.element
        val badgeSize = minOf(60f, availableWidth / data.achievements.size)
        
        data.achievements.take(5).forEachIndexed { index, achievement ->
            val badgeX = sectionRect.left + padding.element + index * (badgeSize + padding.element)
            val badgeY = titleY + padding.element
            
            renderAchievementBadge(canvas, config, badgeX, badgeY, badgeSize, achievement)
        }
        
        recyclePaint(titlePaint)
    }
    
    /**
     * Renders footer section with motivational content and branding
     */
    private fun renderFooterSection(
        data: PngExportData,
        config: PngLayoutConfig,
        y: Float,
        height: Float
    ) {
        val canvas = this.canvas ?: return
        val padding = config.padding
        
        val footerRect = RectF(
            padding.outer.toFloat(),
            y,
            config.canvasWidth - padding.outer.toFloat(),
            y + height
        )
        
        // Motivational message
        val message = data.customization.personalMessage 
            ?: getMotivationalMessage(data.statistics.completionRate)
        
        val messagePaint = getPaint().apply {
            color = adjustAlpha(config.colorPalette.onBackground, 0.8f)
            textSize = config.typography.bodySize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = footerRect.centerX()
        val centerY = footerRect.centerY()
        
        canvas.drawText(message, centerX, centerY, messagePaint)
        
        // Export date
        val datePaint = getPaint().apply {
            color = adjustAlpha(config.colorPalette.onBackground, 0.6f)
            textSize = config.typography.captionSize
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val dateText = "Generated on ${data.metadata.exportDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        canvas.drawText(dateText, centerX, centerY + config.typography.bodySize + padding.text, datePaint)
        
        recyclePaint(messagePaint)
        recyclePaint(datePaint)
    }
    
    /**
     * Utility methods for rendering components
     */
    
    private fun renderCardBackground(canvas: Canvas, rect: RectF, @ColorInt color: Int) {
        val cardPaint = getPaint().apply {
            this.color = color
            isAntiAlias = true
            setShadowLayer(SHADOW_RADIUS, 0f, 2f, adjustAlpha(Color.BLACK, 0.1f))
        }
        
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, cardPaint)
        recyclePaint(cardPaint)
    }
    
    private fun renderAppLogo(canvas: Canvas, x: Float, centerY: Float, size: Float): Float {
        // Placeholder for app logo rendering
        val logoPaint = getPaint().apply {
            color = Color.parseColor("#FF6B35") // Brand color
            isAntiAlias = true
        }
        
        val logoRect = RectF(x, centerY - size/2, x + size, centerY + size/2)
        canvas.drawRoundRect(logoRect, size * 0.2f, size * 0.2f, logoPaint)
        
        recyclePaint(logoPaint)
        return size
    }
    
    private fun renderHeatmapLegend(
        canvas: Canvas,
        config: PngLayoutConfig,
        sectionRect: RectF,
        legend: HeatmapLegend
    ) {
        val legendSize = 12f
        val legendSpacing = 4f
        val legendY = sectionRect.bottom - config.padding.element - legendSize
        
        var currentX = sectionRect.right - config.padding.element - legend.intensityLevels.size * (legendSize + legendSpacing)
        
        legend.intensityLevels.forEach { level ->
            val legendPaint = getPaint().apply {
                color = level.color
                isAntiAlias = true
            }
            
            val legendRect = RectF(currentX, legendY, currentX + legendSize, legendY + legendSize)
            canvas.drawRoundRect(legendRect, 2f, 2f, legendPaint)
            
            currentX += legendSize + legendSpacing
            recyclePaint(legendPaint)
        }
    }
    
    private fun renderAchievementBadge(
        canvas: Canvas,
        config: PngLayoutConfig,
        x: Float,
        y: Float,
        size: Float,
        achievement: Achievement
    ) {
        // Badge background
        val badgePaint = getPaint().apply {
            color = config.colorPalette.primary
            isAntiAlias = true
        }
        
        val badgeRect = RectF(x, y, x + size, y + size)
        canvas.drawRoundRect(badgeRect, size * 0.2f, size * 0.2f, badgePaint)
        
        // Badge icon/emoji (simplified)
        val iconPaint = getPaint().apply {
            color = config.colorPalette.onPrimary
            textSize = size * 0.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = badgeRect.centerX()
        val centerY = badgeRect.centerY() + iconPaint.textSize / 3
        
        canvas.drawText(achievement.iconResource, centerX, centerY, iconPaint)
        
        recyclePaint(badgePaint)
        recyclePaint(iconPaint)
    }
    
    /**
     * Helper methods for memory and resource management
     */
    
    private fun getPaint(): Paint {
        return if (paintPool.isNotEmpty()) {
            paintPool.removeAt(paintPool.lastIndex).apply { reset() }
        } else {
            Paint()
        }
    }
    
    private fun recyclePaint(paint: Paint) {
        if (paintPool.size < 20) { // Limit pool size
            paint.reset()
            paintPool.add(paint)
        }
    }
    
    private fun getPath(): Path {
        return if (pathPool.isNotEmpty()) {
            pathPool.removeAt(pathPool.lastIndex).apply { reset() }
        } else {
            Path()
        }
    }
    
    private fun recyclePath(path: Path) {
        if (pathPool.size < 10) {
            path.reset()
            pathPool.add(path)
        }
    }
    
    /**
     * Color and theming utilities
     */
    
    private fun getColorPalette(theme: PngTheme, scheme: ColorScheme): ColorPalette {
        return when (theme) {
            PngTheme.LIGHT -> getLightColorPalette(scheme)
            PngTheme.DARK -> getDarkColorPalette(scheme)
            PngTheme.HIGH_CONTRAST -> getHighContrastPalette()
            PngTheme.MINIMAL -> getMinimalPalette()
        }
    }
    
    private fun getLightColorPalette(scheme: ColorScheme): ColorPalette {
        return ColorPalette(
            primary = Color.parseColor("#6750A4"),
            onPrimary = Color.parseColor("#FFFFFF"),
            secondary = Color.parseColor("#625B71"),
            onSecondary = Color.parseColor("#FFFFFF"),
            background = Color.parseColor("#FFFBFE"),
            onBackground = Color.parseColor("#1C1B1F"),
            surface = Color.parseColor("#FFFFFF"),
            onSurface = Color.parseColor("#1C1B1F"),
            success = Color.parseColor("#4CAF50"),
            warning = Color.parseColor("#FF9800"),
            error = Color.parseColor("#F44336"),
            heatmapColors = listOf(
                Color.parseColor("#E8F5E8"),
                Color.parseColor("#C8E6C9"),
                Color.parseColor("#A5D6A7"),
                Color.parseColor("#81C784"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#4CAF50")
            )
        )
    }
    
    private fun getDarkColorPalette(scheme: ColorScheme): ColorPalette {
        return ColorPalette(
            primary = Color.parseColor("#D0BCFF"),
            onPrimary = Color.parseColor("#381E72"),
            secondary = Color.parseColor("#CCC2DC"),
            onSecondary = Color.parseColor("#332D41"),
            background = Color.parseColor("#1C1B1F"),
            onBackground = Color.parseColor("#E6E1E5"),
            surface = Color.parseColor("#2B2930"),
            onSurface = Color.parseColor("#E6E1E5"),
            success = Color.parseColor("#81C784"),
            warning = Color.parseColor("#FFB74D"),
            error = Color.parseColor("#EF5350"),
            heatmapColors = listOf(
                Color.parseColor("#2B2D42"),
                Color.parseColor("#414465"),
                Color.parseColor("#565B88"),
                Color.parseColor("#6B72AB"),
                Color.parseColor("#8089CE"),
                Color.parseColor("#95A0F1")
            )
        )
    }
    
    private fun getHighContrastPalette(): ColorPalette {
        return ColorPalette(
            primary = Color.parseColor("#000000"),
            onPrimary = Color.parseColor("#FFFFFF"),
            secondary = Color.parseColor("#666666"),
            onSecondary = Color.parseColor("#FFFFFF"),
            background = Color.parseColor("#FFFFFF"),
            onBackground = Color.parseColor("#000000"),
            surface = Color.parseColor("#F5F5F5"),
            onSurface = Color.parseColor("#000000"),
            success = Color.parseColor("#000000"),
            warning = Color.parseColor("#000000"),
            error = Color.parseColor("#000000"),
            heatmapColors = listOf(
                Color.parseColor("#F5F5F5"),
                Color.parseColor("#CCCCCC"),
                Color.parseColor("#999999"),
                Color.parseColor("#666666"),
                Color.parseColor("#333333"),
                Color.parseColor("#000000")
            )
        )
    }
    
    private fun getMinimalPalette(): ColorPalette {
        return ColorPalette(
            primary = Color.parseColor("#8E8E93"),
            onPrimary = Color.parseColor("#FFFFFF"),
            secondary = Color.parseColor("#C7C7CC"),
            onSecondary = Color.parseColor("#000000"),
            background = Color.parseColor("#FFFFFF"),
            onBackground = Color.parseColor("#000000"),
            surface = Color.parseColor("#F2F2F7"),
            onSurface = Color.parseColor("#000000"),
            success = Color.parseColor("#8E8E93"),
            warning = Color.parseColor("#8E8E93"),
            error = Color.parseColor("#8E8E93"),
            heatmapColors = listOf(
                Color.parseColor("#F2F2F7"),
                Color.parseColor("#E5E5EA"),
                Color.parseColor("#D1D1D6"),
                Color.parseColor("#C7C7CC"),
                Color.parseColor("#AEAEB2"),
                Color.parseColor("#8E8E93")
            )
        )
    }
    
    private fun interpolateHeatmapColor(intensity: Float, colors: List<Int>): Int {
        if (colors.isEmpty()) return Color.GRAY
        if (intensity <= 0f) return colors.first()
        if (intensity >= 1f) return colors.last()
        
        val scaledIntensity = intensity * (colors.size - 1)
        val index = scaledIntensity.toInt()
        val fraction = scaledIntensity - index
        
        val startColor = colors[index]
        val endColor = colors.getOrElse(index + 1) { colors.last() }
        
        return interpolateColors(startColor, endColor, fraction)
    }
    
    private fun interpolateColors(startColor: Int, endColor: Int, fraction: Float): Int {
        val r1 = Color.red(startColor)
        val g1 = Color.green(startColor)
        val b1 = Color.blue(startColor)
        val a1 = Color.alpha(startColor)
        
        val r2 = Color.red(endColor)
        val g2 = Color.green(endColor)
        val b2 = Color.blue(endColor)
        val a2 = Color.alpha(endColor)
        
        val r = (r1 + fraction * (r2 - r1)).toInt()
        val g = (g1 + fraction * (g2 - g1)).toInt()
        val b = (b1 + fraction * (b2 - b1)).toInt()
        val a = (a1 + fraction * (a2 - a1)).toInt()
        
        return Color.argb(a, r, g, b)
    }
    
    private fun adjustAlpha(@ColorInt color: Int, alpha: Float): Int {
        val newAlpha = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }
    
    /**
     * Text and formatting utilities
     */
    
    private fun formatDateRange(dateRange: DateRange): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd")
        return "${dateRange.startDate} - ${dateRange.endDate}"
    }
    
    private fun getMotivationalMessage(completionRate: Double): String {
        return when {
            completionRate >= 0.9 -> "Outstanding consistency! You're building incredible habits! 🏆"
            completionRate >= 0.7 -> "Great progress! Keep up the momentum! 💪"
            completionRate >= 0.5 -> "You're on the right track! Every day counts! 🌟"
            completionRate >= 0.3 -> "Small steps lead to big changes! Keep going! 🚀"
            else -> "Every journey starts with a single step! 🌱"
        }
    }
    
    /**
     * Compression and cleanup
     */
    
    private fun compressToPng(): ByteArray {
        val bitmap = currentBitmap ?: throw RenderingException("No bitmap available for compression")
        
        return ByteArrayOutputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, BITMAP_QUALITY, stream)) {
                throw RenderingException("Failed to compress bitmap to PNG")
            }
            stream.toByteArray()
        }
    }
    
    private fun cleanup() {
        canvas = null
        currentBitmap?.recycle()
        currentBitmap = null
        
        // Clear pools
        paintPool.clear()
        pathPool.clear()
        
        // Suggest garbage collection
        System.gc()
    }
}

/**
 * Data classes for rendering
 */
private data class StatisticItem(
    val label: String,
    val value: String,
    @ColorInt val accentColor: Int
)
