package com.habittracker.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max

/**
 * Professional Responsive Layout System
 * 
 * Automatically adapts to different screen sizes and orientations
 * Following Material 3 adaptive design guidelines
 */

/**
 * Responsive grid system for dynamic layouts
 */
@Composable
fun ResponsiveGrid(
    modifier: Modifier = Modifier,
    columns: ResponsiveInt = ResponsiveInt(compact = 1, medium = 2, expanded = 3),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(DesignTokens.Spacing.medium),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(DesignTokens.Spacing.medium),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: LazyGridScope.() -> Unit
) {
    val screenSize = LocalConfiguration.current.screenWidthDp.dp
    val actualColumns = when {
        screenSize < DesignTokens.Breakpoints.compact -> columns.compact
        screenSize < DesignTokens.Breakpoints.medium -> columns.medium
        else -> columns.expanded
    }

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(actualColumns),
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = itemContent
    )
}

/**
 * Responsive spacing that adapts to screen size
 */
@Composable
fun responsiveSpacing(
    compact: Dp = DesignTokens.Spacing.small,
    medium: Dp = DesignTokens.Spacing.medium,
    expanded: Dp = DesignTokens.Spacing.large
): Dp {
    val screenSize = LocalConfiguration.current.screenWidthDp.dp
    return when {
        screenSize < DesignTokens.Breakpoints.compact -> compact
        screenSize < DesignTokens.Breakpoints.medium -> medium
        else -> expanded
    }
}

/**
 * Responsive padding that adapts to screen size
 */
@Composable
fun responsivePadding(
    compact: PaddingValues = PaddingValues(12.dp),
    medium: PaddingValues = PaddingValues(16.dp),
    expanded: PaddingValues = PaddingValues(24.dp)
): PaddingValues {
    val screenSize = LocalConfiguration.current.screenWidthDp.dp
    return when {
        screenSize < DesignTokens.Breakpoints.compact -> compact
        screenSize < DesignTokens.Breakpoints.medium -> medium
        else -> expanded
    }
}

enum class WindowWidthClass { Compact, Medium, Expanded }

@Composable
fun rememberWindowWidthClass(): WindowWidthClass {
    val width = LocalConfiguration.current.screenWidthDp.dp
    return when {
        width < DesignTokens.Breakpoints.compact -> WindowWidthClass.Compact
        width < DesignTokens.Breakpoints.medium -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
}

@Composable
fun rememberResponsiveHorizontalPadding(
    compact: Dp = 12.dp,
    medium: Dp = 16.dp,
    expanded: Dp = 24.dp
): Dp {
    return when (rememberWindowWidthClass()) {
        WindowWidthClass.Compact -> compact
        WindowWidthClass.Medium -> medium
        WindowWidthClass.Expanded -> expanded
    }
}

@Composable
fun rememberAdaptiveColumnCount(
    minItemWidth: Dp,
    horizontalPadding: Dp,
    itemSpacing: Dp = DesignTokens.Spacing.medium,
    maxColumns: Int = 4
): Int {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val availableWidth = max(0f, (screenWidth - (horizontalPadding * 2)).value)
    val target = (minItemWidth + itemSpacing).value
    val computed = if (target > 0f) floor(availableWidth / target).toInt() else 1
    return computed.coerceIn(1, maxColumns)
}

fun LazyGridScope.fullSpanItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        content()
    }
}

/**
 * Container for responsive integer values
 */
data class ResponsiveInt(
    val compact: Int,
    val medium: Int = compact,
    val expanded: Int = medium
)

/**
 * Container for responsive Dp values
 */
data class ResponsiveDp(
    val compact: Dp,
    val medium: Dp = compact,
    val expanded: Dp = medium
)

/**
 * Adaptive layout composable that switches between layouts based on screen size
 */
@Composable
fun AdaptiveLayout(
    compactLayout: @Composable () -> Unit,
    mediumLayout: @Composable () -> Unit = compactLayout,
    expandedLayout: @Composable () -> Unit = mediumLayout
) {
    val screenSize = LocalConfiguration.current.screenWidthDp.dp
    when {
        screenSize < DesignTokens.Breakpoints.compact -> compactLayout()
        screenSize < DesignTokens.Breakpoints.medium -> mediumLayout()
        else -> expandedLayout()
    }
}

/**
 * Professional overflow handling system
 */
@Composable
fun AdaptiveText(
    text: String,
    maxLines: ResponsiveInt = ResponsiveInt(compact = 2, medium = 3, expanded = 4),
    modifier: Modifier = Modifier
) {
    val screenSize = LocalConfiguration.current.screenWidthDp.dp
    val actualMaxLines = when {
        screenSize < DesignTokens.Breakpoints.compact -> maxLines.compact
        screenSize < DesignTokens.Breakpoints.medium -> maxLines.medium
        else -> maxLines.expanded
    }
    
    // Implementation would use actual Text composable with proper overflow handling
    // This is a simplified version for demonstration
}

/**
 * Smart card layout that adapts to content and screen size
 */
@Composable
fun AdaptiveCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = responsivePadding()
    val elevation = DesignTokens.Elevation.card
    
    // Implementation would use actual Card composable
    Column(
        modifier = modifier.padding(padding)
    ) {
        content()
    }
}
