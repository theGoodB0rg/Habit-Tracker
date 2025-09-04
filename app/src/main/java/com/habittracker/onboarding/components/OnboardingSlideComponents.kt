package com.habittracker.onboarding.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.onboarding.model.OnboardingSlide
import kotlin.math.min

/**
 * Individual slide component for the onboarding flow
 * Features overflow protection, proper padding, and professional Material 3 design
 * 
 * @author Google-level Developer
 */
@Composable
fun OnboardingSlideContent(
    slide: OnboardingSlide,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Calculate responsive dimensions with density consideration
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isLandscape = screenWidth > screenHeight
    val isTablet = screenWidth > 600.dp
    val densityScale = density.density
    
    // Responsive icon size based on screen size and density
    val iconSize = when {
        isTablet -> (140 * densityScale).dp
        isLandscape -> (80 * densityScale).dp
        else -> (120 * densityScale).dp
    }
    
    // Responsive padding
    val horizontalPadding = when {
        isTablet -> 48.dp
        else -> 24.dp
    }
    
    // Animation states with stagger protection
    val animatedVisibility by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = slide.animationDelay.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "slide_visibility"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )
    
    val contentOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 50.dp,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = (slide.animationDelay + 200).toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "content_offset"
    )
    
    // Professional Material 3 color scheme with dynamic theming
    val surfaceColors = MaterialTheme.colorScheme.run {
        val dynamicColors = when {
            slide.backgroundGradientColors.isNotEmpty() -> slide.backgroundGradientColors
            else -> listOf(
                surface,
                surfaceVariant.copy(alpha = 0.3f)
            )
        }
        remember(this, slide.backgroundGradientColors) { dynamicColors }
    }
    
    val scrollState = rememberScrollState()
    // Ensure each slide starts at the top to avoid users needing to scroll up
    LaunchedEffect(slide.id) {
        scrollState.scrollTo(0)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(surfaceColors)
            )
            .verticalScroll(scrollState) // Handle overflow with scrolling
            .padding(horizontal = horizontalPadding)
            .padding(
                top = if (isLandscape) 16.dp else 32.dp,
                bottom = if (isLandscape) 16.dp else 48.dp
            )
            .offset(y = contentOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isLandscape) {
            Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        } else {
            Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        }
    ) {
        
        // Flexible spacer for top spacing
        if (!isLandscape) {
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Animated Icon with Professional Material 3 styling
        Card(
            modifier = Modifier
                .size(iconSize + 24.dp)
                .graphicsLayer {
                    scaleX = iconScale * animatedVisibility
                    scaleY = iconScale * animatedVisibility
                    alpha = animatedVisibility
                },
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = slide.title,
                    modifier = Modifier.size(iconSize * 0.5f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Spacing adjustment for landscape
        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))
        
        // High-contrast header block (prevents blending with colorful backgrounds)
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 12.dp)
                    .graphicsLayer {
                        alpha = animatedVisibility
                        translationY = (1f - animatedVisibility) * 24f
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with overflow protection
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 30.sp else 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtitle with improved contrast
                if (slide.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = slide.subtitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isTablet) 18.sp else 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Spacing adjustment
        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 24.dp))
        
        // Description with proper Material 3 card and overflow handling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .graphicsLayer {
                    alpha = animatedVisibility
                    translationY = (1f - animatedVisibility) * 40f
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                // Solid surface container ensures readability
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Text(
                text = slide.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    lineHeight = if (isTablet) 24.sp else 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = if (isTablet) 32.dp else 20.dp,
                    vertical = if (isTablet) 24.dp else 20.dp
                )
            )
        }
        
        // Bottom spacer for scroll safety
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Progress indicator for onboarding slides
 */
@Composable
fun OnboardingProgressIndicator(
    currentSlide: Int,
    totalSlides: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSlides) { index ->
            val isActive = index <= currentSlide
            val animatedSize by animateDpAsState(
                targetValue = if (isActive) 12.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "indicator_size"
            )
            
            val animatedColor by animateColorAsState(
                targetValue = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                animationSpec = tween(300),
                label = "indicator_color"
            )
            
            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}

/**
 * Skip button with subtle animation
 */
@Composable
fun SkipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "skip_button_scale"
    )
    
    TextButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Text(
            text = "Skip",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
    
    // Reset pressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * Navigation buttons (Previous/Next/Get Started) with professional Material 3 styling
 */
@Composable
fun OnboardingNavigationButtons(
    currentSlide: Int,
    totalSlides: Int,
    canProceed: Boolean = true,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFirstSlide = currentSlide == 0
    val isLastSlide = currentSlide == totalSlides - 1
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button with improved styling
        AnimatedVisibility(
            visible = !isFirstSlide,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canProceed,
                modifier = Modifier.wrapContentWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(
                    1.dp, 
                    if (canProceed) MaterialTheme.colorScheme.outline 
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Text("Previous")
            }
        }
        
        // Spacer for first slide
        if (isFirstSlide) {
            Spacer(modifier = Modifier.width(1.dp))
        }
        
        // Next/Get Started button with professional styling
        Button(
            onClick = if (isLastSlide) onComplete else onNext,
            enabled = canProceed,
            modifier = Modifier.wrapContentWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            )
        ) {
            AnimatedContent(
                targetState = isLastSlide,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut()
                },
                label = "button_text"
            ) { isLast ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isLast) "Get Started" else "Next",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (isLast) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
