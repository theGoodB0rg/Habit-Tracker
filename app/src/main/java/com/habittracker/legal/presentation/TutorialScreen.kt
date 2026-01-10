package com.habittracker.legal.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.habittracker.legal.domain.HighlightType
import com.habittracker.legal.domain.TutorialStep
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Comprehensive visual tutorial screen with interactive highlights and animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    viewModel: LegalViewModel = hiltViewModel()
) {
    val tutorialState by viewModel.tutorialState.collectAsState()
    // Access tooltip manager to show in-app hints
    val tooltipManager: com.habittracker.onboarding.manager.TooltipManager = androidx.hilt.navigation.compose.hiltViewModel()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.initializeTutorial()
    }
    
    if (tutorialState.isVisible && tutorialState.steps.isNotEmpty()) {
        TutorialOverlay(
            tutorialState = tutorialState,
            onNext = { viewModel.nextTutorialStep() },
            onPrevious = { viewModel.previousTutorialStep() },
            onSkip = { 
                viewModel.skipTutorial()
                navController.navigateUp()
            },
            onComplete = {
                navController.navigateUp()
            },
            onShowInApp = { targetId ->
                // Navigate to main and show the relevant tooltip if possible
                navController.navigate(com.habittracker.ui.navigation.Screen.Main.route) {
                    popUpTo(com.habittracker.ui.navigation.Screen.Main.route) { inclusive = false }
                    launchSingleTop = true
                }
                targetId?.let { id ->
                    // Delay slightly to let MainScreen compose and register anchors
                    scope.launch {
                        kotlinx.coroutines.delay(600)
                        tooltipManager.showTooltip(id)
                    }
                }
            }
        )
    } else {
        // Fallback content or navigation back
        LaunchedEffect(Unit) {
            navController.navigateUp()
        }
    }
}

@Composable
private fun TutorialOverlay(
    tutorialState: TutorialUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onShowInApp: (targetId: String?) -> Unit
) {
    val currentStep = tutorialState.currentStep ?: return
    
    // Get screen dimensions for responsive design
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isCompactScreen = screenHeight < 700.dp
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.85f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Prevent clicks from passing through */ }
    ) {
        // Highlight overlay
        TutorialHighlight(
            step = currentStep,
            modifier = Modifier.fillMaxSize()
        )
        
        // Main content in a scrollable column for better device compatibility
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (screenWidth < 600.dp) 12.dp else 24.dp,
                    vertical = if (isCompactScreen) 16.dp else 24.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress indicator at top
            TutorialProgressIndicator(
                progress = tutorialState.progressPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isCompactScreen) 8.dp else 16.dp)
            )
            
            // Tutorial content card - flexible sizing
            TutorialContentCard(
                step = currentStep,
                tutorialState = tutorialState,
                onNext = onNext,
                onPrevious = onPrevious,
                onSkip = onSkip,
                onComplete = onComplete,
                onShowInApp = onShowInApp,
                isCompactScreen = isCompactScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )
        }
    }
}

@Composable
private fun TutorialHighlight(
    step: TutorialStep,
    modifier: Modifier = Modifier
) {
    // Animated highlight effect with density-aware scaling
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val scaleFactor = remember(density) { density.density }
    val isCompactScreen = configuration.screenHeightDp < 700
    
    val baseHighlightRadius = if (isCompactScreen) 60.dp else 80.dp
    
    val animatedScale by rememberInfiniteTransition(label = "highlight_scale").animateFloat(
        initialValue = 1f,
        targetValue = 1f + (0.08f * scaleFactor), // Reduced scaling for compact screens
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val animatedAlpha by rememberInfiniteTransition(label = "highlight_alpha").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Canvas(modifier = modifier) {
        // Position highlight based on screen size
        val highlightCenter = Offset(
            size.width / 2, 
            if (isCompactScreen) size.height * 0.25f else size.height / 3
        )
        val highlightRadius = baseHighlightRadius.toPx()
        
        when (step.highlightType) {
            HighlightType.CIRCLE -> {
                // Outer glow
                drawCircle(
                    color = Color.White.copy(alpha = animatedAlpha * 0.3f),
                    radius = highlightRadius * animatedScale * 1.3f,
                    center = highlightCenter
                )
                
                // Main highlight ring
                drawCircle(
                    color = Color.White.copy(alpha = animatedAlpha),
                    radius = highlightRadius * animatedScale,
                    center = highlightCenter,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Inner fill
                drawCircle(
                    color = Color.White.copy(alpha = animatedAlpha * 0.2f),
                    radius = highlightRadius * animatedScale * 0.7f,
                    center = highlightCenter
                )
            }
            
            HighlightType.RECTANGLE -> {
                val rectSize = Size(
                    if (isCompactScreen) 140.dp.toPx() else 160.dp.toPx(),
                    if (isCompactScreen) 60.dp.toPx() else 80.dp.toPx()
                )
                drawRect(
                    color = Color.White.copy(alpha = animatedAlpha),
                    topLeft = Offset(
                        highlightCenter.x - rectSize.width / 2,
                        highlightCenter.y - rectSize.height / 2
                    ),
                    size = rectSize,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            
            HighlightType.ROUNDED_RECTANGLE -> {
                val rectSize = Size(
                    if (isCompactScreen) 180.dp.toPx() else 200.dp.toPx(),
                    if (isCompactScreen) 80.dp.toPx() else 100.dp.toPx()
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = animatedAlpha),
                    topLeft = Offset(
                        highlightCenter.x - rectSize.width / 2,
                        highlightCenter.y - rectSize.height / 2
                    ),
                    size = rectSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            
            HighlightType.NONE -> {
                // No highlight, just the overlay
            }
        }
        
        // Draw pointer arrow if needed and space allows
        if (step.highlightType != HighlightType.NONE && !isCompactScreen) {
            drawTutorialPointer(
                center = highlightCenter,
                color = Color.White.copy(alpha = animatedAlpha * 0.8f)
            )
        }
    }
}

private fun DrawScope.drawTutorialPointer(
    center: Offset,
    color: Color
) {
    val pointerPath = Path().apply {
        moveTo(center.x, center.y + 120.dp.toPx())
        lineTo(center.x - 15.dp.toPx(), center.y + 90.dp.toPx())
        lineTo(center.x + 15.dp.toPx(), center.y + 90.dp.toPx())
        close()
    }
    
    drawPath(
        path = pointerPath,
        color = color
    )
}

@Composable
private fun TutorialContentCard(
    step: TutorialStep,
    tutorialState: TutorialUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onShowInApp: (targetId: String?) -> Unit,
    isCompactScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Create dynamic card content based on step
    val context = LocalContext.current
    val stepTitle = remember(step) { context.getString(step.titleRes) }
    val stepDescription = remember(step) { context.getString(step.descriptionRes) }
    
    // Responsive sizing
    val iconSize = if (isCompactScreen) 48.dp else 64.dp
    val cardPadding = if (isCompactScreen) 16.dp else 20.dp
    val verticalSpacing = if (isCompactScreen) 8.dp else 12.dp
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step illustration with better contrast
            Box(
                modifier = Modifier
                    .size(iconSize + 16.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getStepIcon(tutorialState.currentStepIndex),
                    contentDescription = stepTitle,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(verticalSpacing + 4.dp))
            
            // Step title with better typography
            Text(
                text = stepTitle,
                style = if (isCompactScreen) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isCompactScreen) 2 else 3
            )
            
            Spacer(modifier = Modifier.height(verticalSpacing))
            
            // Step description with improved readability
            Text(
                text = stepDescription,
                style = if (isCompactScreen) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = if (isCompactScreen) {
                    MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                } else {
                    MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                },
                maxLines = if (isCompactScreen) 3 else 4
            )
            
            Spacer(modifier = Modifier.height(verticalSpacing + 8.dp))
            
            // Action buttons
            TutorialActionButtons(
                tutorialState = tutorialState,
                onNext = onNext,
                onPrevious = onPrevious,
                onSkip = onSkip,
                onComplete = onComplete,
                onShowInApp = { onShowInApp(step.targetViewId) },
                isCompactScreen = isCompactScreen
            )
        }
    }
}

@Composable
private fun TutorialActionButtons(
    tutorialState: TutorialUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onShowInApp: () -> Unit,
    isCompactScreen: Boolean = false
) {
    val buttonHeight = if (isCompactScreen) 40.dp else 48.dp
    val buttonSpacing = if (isCompactScreen) 8.dp else 12.dp
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main action button (Next/Complete) - full width for better touch targets
        Button(
            onClick = if (tutorialState.isLastStep) onComplete else onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            shape = RoundedCornerShape(buttonHeight / 2),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (tutorialState.isLastStep) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(if (isCompactScreen) 16.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Complete Tutorial",
                    style = if (isCompactScreen) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
            } else {
                Text(
                    "Next Step",
                    style = if (isCompactScreen) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(if (isCompactScreen) 16.dp else 18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(buttonSpacing))
        
        // Secondary actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show in app (contextual deep-link to highlighted UI)
            TextButton(
                onClick = onShowInApp,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(buttonHeight)
            ) {
                Text(
                    if (tutorialState.isLastStep) "Open App" else "Show in app",
                    style = if (isCompactScreen) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.labelLarge
                    }
                )
            }
            // Skip button
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                modifier = Modifier.height(buttonHeight)
            ) {
                Text(
                    "Skip Tutorial",
                    style = if (isCompactScreen) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.labelLarge
                    }
                )
            }
            
            // Previous button (only show if not first step)
            if (!tutorialState.isFirstStep) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.height(buttonHeight),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(if (isCompactScreen) 16.dp else 18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Back",
                        style = if (isCompactScreen) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        }
                    )
                }
            } else {
                // Placeholder to maintain layout balance
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

@Composable
private fun TutorialProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactScreen = configuration.screenHeightDp < 700
    val progressWidth = if (configuration.screenWidthDp < 600) {
        (configuration.screenWidthDp * 0.8f).dp
    } else {
        240.dp
    }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isCompactScreen) 12.dp else 16.dp,
                vertical = if (isCompactScreen) 8.dp else 12.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tutorial Progress",
                style = if (isCompactScreen) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(if (isCompactScreen) 6.dp else 8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(progressWidth)
                    .height(if (isCompactScreen) 6.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(if (isCompactScreen) 4.dp else 6.dp))
            
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = if (isCompactScreen) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper functions for demo content
private fun getStepTitle(stepIndex: Int): String {
    return when (stepIndex) {
        0 -> "Welcome to Habit Tracker!"
        1 -> "Adding Your First Habit"
        2 -> "Tracking Your Progress"
        3 -> "Building Streaks"
        4 -> "Staying Motivated"
        else -> "Tutorial Step ${stepIndex + 1}"
    }
}

private fun getStepDescription(stepIndex: Int): String {
    return when (stepIndex) {
        0 -> "This tutorial will guide you through all the features of Habit Tracker. Let's build better habits together!"
        1 -> "Tap the '+' button to create a new habit. Choose an icon, set a name, and customize your tracking preferences."
        2 -> "Mark habits as complete by tapping the circle next to each habit. Your progress will be automatically tracked."
        3 -> "Build and maintain streaks by completing habits consistently. The longer your streak, the more motivated you'll become!"
        4 -> "Use the insights and reminders to stay on track. You've got this!"
        else -> "Continue through the tutorial to learn more about this feature."
    }
}

private fun getStepIcon(stepIndex: Int): ImageVector {
    return when (stepIndex) {
        0 -> Icons.Default.Star
        1 -> Icons.Default.Add
        2 -> Icons.Default.CheckCircle
        3 -> Icons.AutoMirrored.Filled.TrendingUp
        4 -> Icons.Default.EmojiEvents
        else -> Icons.Default.Info
    }
}
