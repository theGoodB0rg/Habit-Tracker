package com.habittracker.onboarding.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.habittracker.onboarding.model.TooltipConfig
import com.habittracker.onboarding.model.TooltipPosition
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Advanced tooltip system for in-app guided tours
 * Features spotlight effect, smooth animations, and flexible positioning
 * 
 * @author Google-level Developer
 */
@Composable
fun TooltipOverlay(
    tooltipConfig: TooltipConfig,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier // Reserved for future customization
) {
    if (!isVisible) return
    
    // Animation states
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.7f else 0f,
        animationSpec = tween(400),
        label = "overlay_alpha"
    )
    
    val tooltipScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tooltip_scale"
    )
    
    val tooltipAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, delayMillis = 200),
        label = "tooltip_alpha"
    )
    
    // Popup for overlay effect
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = onDismiss
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .background(Color.Black.copy(alpha = overlayAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                }
        ) {
            // Try to position near target if coordinates are known
            val targetRect = TooltipCoordinateManager.getTarget(tooltipConfig.targetComposableKey)
            val density = LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val marginPx = with(density) { 16.dp.toPx() }
            var tooltipSizePx by remember { mutableStateOf(IntSize.Zero) }

            targetRect?.let {
                SpotlightHighlight(
                    rect = it,
                    highlightColor = tooltipConfig.highlightColor,
                    density = density
                )
            }

            val tooltipOffset = remember(
                targetRect,
                tooltipSizePx,
                screenWidthPx,
                screenHeightPx,
                marginPx,
                tooltipConfig.position
            ) {
                calculateTooltipOffset(
                    targetRect = targetRect,
                    tooltipSize = tooltipSizePx,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    marginPx = marginPx,
                    position = tooltipConfig.position
                )
            }

            TooltipCard(
                config = tooltipConfig,
                scale = tooltipScale,
                alpha = tooltipAlpha,
                onDismiss = onDismiss,
                modifier = Modifier
                    .offset { tooltipOffset }
                    .onGloballyPositioned { coordinates ->
                        tooltipSizePx = coordinates.size
                    }
            )
        }
    }
}

@Composable
private fun TooltipCard(
    config: TooltipConfig,
    scale: Float,
    alpha: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 320.dp)
            .padding(24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tooltip",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = config.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text("Got it!")
                }
            }
        }
    }
}

@Composable
private fun SpotlightHighlight(
    rect: Rect,
    highlightColor: Color,
    density: Density,
    modifier: Modifier = Modifier
) {
    val paddingPx = with(density) { 12.dp.toPx() }
    val borderWidth = with(density) { 2.dp.toPx() }
    val cornerRadiusPx = with(density) { 16.dp.toPx() }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val expandedLeft = (rect.left - paddingPx).coerceAtLeast(0f)
        val expandedTop = (rect.top - paddingPx).coerceAtLeast(0f)
        val expandedRight = rect.right + paddingPx
        val expandedBottom = rect.bottom + paddingPx
        val expandedWidth = max(1f, expandedRight - expandedLeft)
        val expandedHeight = max(1f, expandedBottom - expandedTop)
        val center = Offset(
            x = expandedLeft + expandedWidth / 2f,
            y = expandedTop + expandedHeight / 2f
        )
        val glowRadius = max(expandedWidth, expandedHeight) / 2f * 1.6f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    highlightColor.copy(alpha = 0.35f),
                    highlightColor.copy(alpha = 0f)
                ),
                center = center,
                radius = glowRadius
            )
        )
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(expandedLeft, expandedTop),
            size = Size(expandedWidth, expandedHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            color = highlightColor.copy(alpha = 0.55f),
            topLeft = Offset(expandedLeft, expandedTop),
            size = Size(expandedWidth, expandedHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = borderWidth)
        )
    }
}

/**
 * Spotlight effect for highlighting specific UI elements
 */
@Composable
fun SpotlightEffect(
    @Suppress("UNUSED_PARAMETER") targetKey: String, // Reserved for coordinate tracking system
    @Suppress("UNUSED_PARAMETER") highlightColor: Color, // Reserved for custom highlight colors
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // This would typically integrate with a global coordinate tracking system
    // For now, we'll create a simple highlight overlay
    
    if (!isVisible) return
    
    val spotlightAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "spotlight_alpha"
    )
    
    val pulseScale by animateFloatAsState(
        targetValue = if (isVisible) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = spotlightAlpha
                scaleX = pulseScale
                scaleY = pulseScale
            }
    ) {
        // Spotlight implementation would go here
        // This requires integration with the target component's position
    }
}

/**
 * Tooltip manager for controlling multiple tooltips
 */
@Composable
fun TooltipManager(
    tooltips: List<TooltipConfig>,
    currentTooltipIndex: Int,
    isActive: Boolean,
    onTooltipDismiss: () -> Unit,
    onTooltipComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Guard against invalid indices (including negative) or inactive state
    if (!isActive) return
    if (currentTooltipIndex !in 0 until tooltips.size) return

    val currentTooltip = tooltips[currentTooltipIndex]
    
    // Auto-dismiss after a certain time if configured
    LaunchedEffect(currentTooltipIndex) {
        if (currentTooltip.showOnlyOnce) {
            delay(10000) // Auto-dismiss after 10 seconds
            if (currentTooltipIndex == tooltips.lastIndex) {
                onTooltipComplete()
            } else {
                onTooltipDismiss()
            }
        }
    }
    
    TooltipOverlay(
        tooltipConfig = currentTooltip,
        isVisible = true,
        onDismiss = {
            if (currentTooltipIndex == tooltips.lastIndex) {
                onTooltipComplete()
            } else {
                onTooltipDismiss()
            }
        },
        modifier = modifier
    )
}

/**
 * Hook for tracking composable positions for tooltips
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberTooltipTarget(
    key: String
): Modifier {
    val density = LocalDensity.current
    val densityValue = density.density
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    
    return Modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .onGloballyPositioned { coordinates ->
            // Store coordinates in a global registry for tooltip positioning
            // This would integrate with a tooltip coordinate manager
            // Density is used for accurate pixel-to-dp conversions
            val bounds = Rect(
                offset = coordinates.localToWindow(Offset.Zero),
                size = coordinates.size.toSize()
            )
            TooltipCoordinateManager.updateTarget(
                key = key,
                bounds = bounds,
                density = densityValue,
                bringIntoViewRequester = bringIntoViewRequester
            )
        }
}

/**
 * Global coordinate manager for tooltip positioning
 * In a real implementation, this would be a proper singleton or DI component
 */
private fun calculateTooltipOffset(
    targetRect: Rect?,
    tooltipSize: IntSize,
    screenWidthPx: Float,
    screenHeightPx: Float,
    marginPx: Float,
    position: TooltipPosition
): IntOffset {
    val tooltipWidth = tooltipSize.width.toFloat()
    val tooltipHeight = tooltipSize.height.toFloat()

    if (targetRect == null || tooltipWidth == 0f || tooltipHeight == 0f) {
        val safeX = ((screenWidthPx - tooltipWidth) / 2f).coerceAtLeast(marginPx)
        val safeY = ((screenHeightPx - tooltipHeight) / 2f).coerceAtLeast(marginPx)
        return IntOffset(safeX.roundToInt(), safeY.roundToInt())
    }

    val centerX = targetRect.left + (targetRect.width / 2f)
    val centerY = targetRect.top + (targetRect.height / 2f)

    var x = when (position) {
        TooltipPosition.TOP -> centerX - tooltipWidth / 2f
        TooltipPosition.BOTTOM -> centerX - tooltipWidth / 2f
        TooltipPosition.LEFT -> targetRect.left - tooltipWidth - marginPx
        TooltipPosition.RIGHT -> targetRect.right + marginPx
        TooltipPosition.CENTER -> centerX - tooltipWidth / 2f
        TooltipPosition.TOP_LEFT -> targetRect.left
        TooltipPosition.TOP_RIGHT -> targetRect.right - tooltipWidth
        TooltipPosition.BOTTOM_LEFT -> targetRect.left
        TooltipPosition.BOTTOM_RIGHT -> targetRect.right - tooltipWidth
    }

    var y = when (position) {
        TooltipPosition.TOP -> targetRect.top - tooltipHeight - marginPx
        TooltipPosition.BOTTOM -> targetRect.bottom + marginPx
        TooltipPosition.LEFT -> centerY - tooltipHeight / 2f
        TooltipPosition.RIGHT -> centerY - tooltipHeight / 2f
        TooltipPosition.CENTER -> centerY - tooltipHeight / 2f
        TooltipPosition.TOP_LEFT -> targetRect.top - tooltipHeight - marginPx
        TooltipPosition.TOP_RIGHT -> targetRect.top - tooltipHeight - marginPx
        TooltipPosition.BOTTOM_LEFT -> targetRect.bottom + marginPx
        TooltipPosition.BOTTOM_RIGHT -> targetRect.bottom + marginPx
    }

    val prefersTop = position == TooltipPosition.TOP || position == TooltipPosition.TOP_LEFT || position == TooltipPosition.TOP_RIGHT
    val prefersBottom = position == TooltipPosition.BOTTOM || position == TooltipPosition.BOTTOM_LEFT || position == TooltipPosition.BOTTOM_RIGHT
    val prefersLeft = position == TooltipPosition.LEFT || position == TooltipPosition.TOP_LEFT || position == TooltipPosition.BOTTOM_LEFT
    val prefersRight = position == TooltipPosition.RIGHT || position == TooltipPosition.TOP_RIGHT || position == TooltipPosition.BOTTOM_RIGHT

    if (prefersTop && y < marginPx) {
        y = targetRect.bottom + marginPx
    }
    if (prefersBottom && (y + tooltipHeight + marginPx) > screenHeightPx) {
        y = targetRect.top - tooltipHeight - marginPx
    }
    if (prefersLeft && x < marginPx) {
        x = targetRect.right + marginPx
    }
    if (prefersRight && (x + tooltipWidth + marginPx) > screenWidthPx) {
        x = targetRect.left - tooltipWidth - marginPx
    }

    val horizontalMax = screenWidthPx - tooltipWidth - marginPx
    val verticalMax = screenHeightPx - tooltipHeight - marginPx

    x = when {
        tooltipWidth <= 0f -> x
        horizontalMax <= marginPx -> (screenWidthPx - tooltipWidth) / 2f
        else -> x.coerceIn(marginPx, horizontalMax)
    }

    y = when {
        tooltipHeight <= 0f -> y
        verticalMax <= marginPx -> (screenHeightPx - tooltipHeight) / 2f
        else -> y.coerceIn(marginPx, verticalMax)
    }

    return IntOffset(x.roundToInt(), y.roundToInt())
}
@OptIn(ExperimentalFoundationApi::class)
object TooltipCoordinateManager {
    private val coordinates = mutableMapOf<String, Rect>()
    private val densityValues = mutableMapOf<String, Float>()
    private val bringIntoViewRequesters = mutableMapOf<String, BringIntoViewRequester>()
    
    fun updateTarget(
        key: String,
        bounds: Rect,
        density: Float = 1f,
        bringIntoViewRequester: BringIntoViewRequester? = null
    ) {
        coordinates[key] = bounds
        densityValues[key] = density
        if (bringIntoViewRequester != null) {
            bringIntoViewRequesters[key] = bringIntoViewRequester
        }
    }
    
    fun getTarget(key: String): Rect? {
        return coordinates[key]
    }
    
    fun getDensity(key: String): Float {
        return densityValues[key] ?: 1f
    }

    suspend fun bringTargetIntoView(key: String) {
        bringIntoViewRequesters[key]?.bringIntoView()
    }
    
    fun clearTargets() {
        coordinates.clear()
        densityValues.clear()
        bringIntoViewRequesters.clear()
    }
}
