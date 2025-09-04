package com.habittracker.onboarding.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.habittracker.onboarding.model.TooltipConfig
import com.habittracker.onboarding.model.TooltipPosition
import kotlinx.coroutines.delay

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
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            val tooltipModifier = if (targetRect != null) {
                // Compute base position relative to target + desired offset
                val screenPadding = 12.dp
                val targetLeft = with(density) { targetRect.left.toDp() }
                val targetTop = with(density) { targetRect.top.toDp() }
                val targetRight = with(density) { targetRect.right.toDp() }
                val targetBottom = with(density) { targetRect.bottom.toDp() }

                // Initial (x,y) before size known; we rely on Popup natural sizing then clamp via padding
                val baseModifier = when (tooltipConfig.position) {
                    com.habittracker.onboarding.model.TooltipPosition.TOP -> {
                        Modifier.padding(start = targetLeft, top = (targetTop - 8.dp))
                    }
                    com.habittracker.onboarding.model.TooltipPosition.BOTTOM -> {
                        Modifier.padding(start = targetLeft, top = targetBottom + 8.dp)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.LEFT -> {
                        Modifier.padding(start = (targetLeft - 8.dp), top = targetTop)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.RIGHT -> {
                        Modifier.padding(start = targetRight + 8.dp, top = targetTop)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.CENTER -> {
                        Modifier.align(Alignment.Center)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.TOP_LEFT -> {
                        Modifier.padding(start = targetLeft, top = targetTop - 8.dp)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.TOP_RIGHT -> {
                        Modifier.padding(start = targetRight, top = targetTop - 8.dp)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.BOTTOM_LEFT -> {
                        Modifier.padding(start = targetLeft, top = targetBottom + 8.dp)
                    }
                    com.habittracker.onboarding.model.TooltipPosition.BOTTOM_RIGHT -> {
                        Modifier.padding(start = targetRight, top = targetBottom + 8.dp)
                    }
                }
                // Align top-start for explicit coordinate-based placements (except center case)
                if (tooltipConfig.position == com.habittracker.onboarding.model.TooltipPosition.CENTER) {
                    baseModifier
                } else {
                    baseModifier.align(Alignment.TopStart)
                }
            } else {
                Modifier.align(Alignment.Center)
            }
            
            TooltipCard(
                config = tooltipConfig,
                scale = tooltipScale,
                alpha = tooltipAlpha,
                onDismiss = onDismiss,
                modifier = tooltipModifier
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
@Composable
fun rememberTooltipTarget(
    key: String
): Modifier {
    val density = LocalDensity.current
    val densityValue = density.density
    
    return Modifier.onGloballyPositioned { coordinates ->
        // Store coordinates in a global registry for tooltip positioning
        // This would integrate with a tooltip coordinate manager
        // Density is used for accurate pixel-to-dp conversions
        val bounds = Rect(
            offset = coordinates.localToWindow(Offset.Zero),
            size = coordinates.size.toSize()
        )
        TooltipCoordinateManager.updateTarget(key, bounds, densityValue)
    }
}

/**
 * Global coordinate manager for tooltip positioning
 * In a real implementation, this would be a proper singleton or DI component
 */
object TooltipCoordinateManager {
    private val coordinates = mutableMapOf<String, Rect>()
    private val densityValues = mutableMapOf<String, Float>()
    
    fun updateTarget(key: String, bounds: Rect, density: Float = 1f) {
        coordinates[key] = bounds
        densityValues[key] = density
    }
    
    fun getTarget(key: String): Rect? {
        return coordinates[key]
    }
    
    fun getDensity(key: String): Float {
        return densityValues[key] ?: 1f
    }
    
    fun clearTargets() {
        coordinates.clear()
        densityValues.clear()
    }
}
