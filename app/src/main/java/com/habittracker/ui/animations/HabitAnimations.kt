package com.habittracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object HabitAnimations {
    
    val slideInFromBottom = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val slideOutToBottom = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val fadeInSlow = fadeIn(
        animationSpec = tween(
            durationMillis = 500,
            easing = EaseOutCubic
        )
    )
    
    val fadeOutSlow = fadeOut(
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInCubic
        )
    )
    
    val scaleInBouncy = scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val scaleOutQuick = scaleOut(
        animationSpec = tween(
            durationMillis = 150,
            easing = EaseInCubic
        )
    )
}

@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = HabitAnimations.scaleInBouncy + HabitAnimations.fadeInSlow,
        exit = HabitAnimations.scaleOutQuick + HabitAnimations.fadeOutSlow,
        modifier = modifier
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            content = content
        )
    }
}

@Composable
fun <T> AnimatedLazyItemPlacement(
    item: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        content(item)
    }
}
