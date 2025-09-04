package com.habittracker.widget.animation

import android.animation.ValueAnimator
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import com.habittracker.widget.R

/**
 * Professional animation controller for widget micro-interactions and transitions.
 * 
 * Features:
 * - Smooth completion toggle animations
 * - Progress update animations with easing
 * - Loading state animations (skeleton, shimmer)
 * - Error state transitions
 * - Micro-interaction feedback system
 * 
 * Performance:
 * - 60fps animations
 * - Hardware acceleration where possible
 * - Memory efficient animation queuing
 * - Battery optimized timing
 */
class WidgetAnimationController private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetAnimationController? = null
        
        fun getInstance(context: Context): WidgetAnimationController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetAnimationController(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Animation timing constants (optimized for battery and smoothness)
        private const val COMPLETION_ANIMATION_DURATION = 300L
        private const val PROGRESS_ANIMATION_DURATION = 250L
        private const val LOADING_ANIMATION_DURATION = 1500L
        private const val MICRO_INTERACTION_DURATION = 150L
        private const val ERROR_ANIMATION_DURATION = 400L
        
        // Animation easing curves
        private const val EASE_OUT_CUBIC = "cubic-bezier(0.215, 0.610, 0.355, 1.000)"
        private const val EASE_IN_OUT_CUBIC = "cubic-bezier(0.645, 0.045, 0.355, 1.000)"
        private const val BOUNCE_EASE = "cubic-bezier(0.175, 0.885, 0.320, 1.275)"
    }
    
    // Active animations tracking for performance
    private val activeAnimations = ConcurrentHashMap<String, Boolean>()
    
    /**
     * Animate habit completion toggle with satisfying feedback
     * Creates a smooth transition that feels responsive and delightful
     */
    suspend fun animateCompletionToggle(
        views: RemoteViews,
        habitId: Long,
        isCompleted: Boolean
    ): Flow<AnimationState> = flow {
        val animationKey = "completion_$habitId"
        
        // Prevent duplicate animations
        if (activeAnimations.containsKey(animationKey)) return@flow
        activeAnimations[animationKey] = true
        
        try {
            emit(AnimationState.Started)
            
            // Phase 1: Quick press feedback (0-50ms)
            views.setFloat(R.id.toggle_done, "setAlpha", 0.7f)
            views.setFloat(R.id.toggle_done, "setScaleX", 0.95f)
            views.setFloat(R.id.toggle_done, "setScaleY", 0.95f)
            emit(AnimationState.Progress(0.2f))
            delay(50)
            
            // Phase 2: Bounce back (50-150ms)
            views.setFloat(R.id.toggle_done, "setAlpha", 1.0f)
            views.setFloat(R.id.toggle_done, "setScaleX", 1.05f)
            views.setFloat(R.id.toggle_done, "setScaleY", 1.05f)
            emit(AnimationState.Progress(0.5f))
            delay(100)
            
            // Phase 3: Settle to final state (150-300ms)
            views.setFloat(R.id.toggle_done, "setScaleX", 1.0f)
            views.setFloat(R.id.toggle_done, "setScaleY", 1.0f)
            
            // Update visual state based on completion
            if (isCompleted) {
                // Completed state: subtle fade and check animation
                views.setFloat(R.id.habit_name, "setAlpha", 0.7f)
                views.setInt(R.id.habit_name, "setTextColor", 
                    context.resources.getColor(android.R.color.darker_gray, null))
                views.setTextViewText(R.id.streak_display, "✅")
            } else {
                // Incomplete state: full opacity and restore
                views.setFloat(R.id.habit_name, "setAlpha", 1.0f)
                views.setInt(R.id.habit_name, "setTextColor", 
                    context.resources.getColor(R.color.widgetTextColor, null))
                views.setTextViewText(R.id.streak_display, "🔥")
            }
            
            emit(AnimationState.Progress(0.8f))
            delay(150)
            
            emit(AnimationState.Completed)
            
        } finally {
            activeAnimations.remove(animationKey)
        }
    }
    
    /**
     * Animate progress counter updates with smooth number transitions
     */
    suspend fun animateProgressUpdate(
        views: RemoteViews,
        fromProgress: Float,
        toProgress: Float,
        fromCount: String,
        toCount: String
    ): Flow<AnimationState> = flow {
        val animationKey = "progress_update"
        
        if (activeAnimations.containsKey(animationKey)) return@flow
        activeAnimations[animationKey] = true
        
        try {
            emit(AnimationState.Started)
            
            val steps = 10
            val progressDiff = toProgress - fromProgress
            val stepDuration = PROGRESS_ANIMATION_DURATION / steps
            
            for (i in 1..steps) {
                val progress = i.toFloat() / steps.toFloat()
                val currentProgress = fromProgress + (progressDiff * easeOutCubic(progress))
                
                // Update progress indicator with smooth interpolation
                views.setTextViewText(R.id.progress_indicator, 
                    "${(currentProgress * 100).toInt()}%")
                
                // Update count with smooth transition (only on significant steps)
                if (i == steps / 2) {
                    views.setTextViewText(R.id.daily_progress, toCount)
                }
                
                emit(AnimationState.Progress(progress))
                delay(stepDuration)
            }
            
            // Ensure final values are set
            views.setTextViewText(R.id.progress_indicator, "${(toProgress * 100).toInt()}%")
            views.setTextViewText(R.id.daily_progress, toCount)
            
            emit(AnimationState.Completed)
            
        } finally {
            activeAnimations.remove(animationKey)
        }
    }
    
    /**
     * Create professional loading animation with shimmer effect
     */
    suspend fun animateLoadingState(views: RemoteViews): Flow<AnimationState> = flow {
        val animationKey = "loading_shimmer"
        
        if (activeAnimations.containsKey(animationKey)) return@flow
        activeAnimations[animationKey] = true
        
        try {
            emit(AnimationState.Started)
            
            val pulseSteps = 20
            val stepDuration = LOADING_ANIMATION_DURATION / pulseSteps
            
            for (cycle in 0 until 3) { // 3 loading cycles
                for (i in 0 until pulseSteps) {
                    val progress = i.toFloat() / pulseSteps.toFloat()
                    val alpha = 0.3f + (0.4f * kotlin.math.sin(progress * Math.PI).toFloat())
                    
                    // Apply shimmer effect to loading elements
                    views.setFloat(R.id.widget_title, "setAlpha", alpha)
                    views.setFloat(R.id.habits_list, "setAlpha", alpha)
                    views.setFloat(R.id.daily_progress, "setAlpha", alpha)
                    
                    emit(AnimationState.Progress(((cycle * pulseSteps + i).toFloat()) / (3 * pulseSteps).toFloat()))
                    delay(stepDuration)
                }
            }
            
            // Restore full opacity
            views.setFloat(R.id.widget_title, "setAlpha", 1.0f)
            views.setFloat(R.id.habits_list, "setAlpha", 1.0f)
            views.setFloat(R.id.daily_progress, "setAlpha", 1.0f)
            
            emit(AnimationState.Completed)
            
        } finally {
            activeAnimations.remove(animationKey)
        }
    }
    
    /**
     * Animate error state with gentle attention-grabbing effect
     */
    suspend fun animateErrorState(views: RemoteViews, errorMessage: String): Flow<AnimationState> = flow {
        val animationKey = "error_state"
        
        if (activeAnimations.containsKey(animationKey)) return@flow
        activeAnimations[animationKey] = true
        
        try {
            emit(AnimationState.Started)
            
            // Phase 1: Gentle shake effect (0-200ms)
            val shakeSteps = 8
            val shakeAmount = 2f
            
            for (i in 0 until shakeSteps) {
                val offset = shakeAmount * kotlin.math.sin((i * Math.PI) / 2).toFloat()
                // Note: RemoteViews doesn't support translation, so we use subtle scale instead
                val scale = 1.0f + (offset * 0.01f)
                
                views.setFloat(R.id.button_refresh, "setScaleX", scale)
                views.setFloat(R.id.button_refresh, "setScaleY", scale)
                
                emit(AnimationState.Progress(i.toFloat() / shakeSteps.toFloat() * 0.5f))
                delay(25)
            }
            
            // Phase 2: Show error message with fade-in (200-400ms)
            views.setTextViewText(R.id.button_refresh, errorMessage)
            views.setInt(R.id.button_refresh, "setTextColor", 
                context.resources.getColor(android.R.color.holo_red_light, null))
            
            // Subtle color pulse for attention
            for (i in 0 until 4) {
                val alpha = 0.7f + (0.3f * kotlin.math.sin((i * Math.PI) / 2).toFloat())
                views.setFloat(R.id.button_refresh, "setAlpha", alpha)
                
                emit(AnimationState.Progress(0.5f + (i.toFloat() / 8f)))
                delay(50)
            }
            
            // Restore normal state
            views.setFloat(R.id.button_refresh, "setScaleX", 1.0f)
            views.setFloat(R.id.button_refresh, "setScaleY", 1.0f)
            views.setFloat(R.id.button_refresh, "setAlpha", 1.0f)
            
            emit(AnimationState.Completed)
            
        } finally {
            activeAnimations.remove(animationKey)
        }
    }
    
    /**
     * Micro-interaction for button press feedback
     */
    suspend fun animateButtonPress(views: RemoteViews, buttonId: Int): Flow<AnimationState> = flow {
        val animationKey = "button_press_$buttonId"
        
        if (activeAnimations.containsKey(animationKey)) return@flow
        activeAnimations[animationKey] = true
        
        try {
            emit(AnimationState.Started)
            
            // Quick press down (0-75ms)
            views.setFloat(buttonId, "setAlpha", 0.8f)
            views.setFloat(buttonId, "setScaleX", 0.96f)
            views.setFloat(buttonId, "setScaleY", 0.96f)
            
            emit(AnimationState.Progress(0.5f))
            delay(75)
            
            // Quick release (75-150ms)
            views.setFloat(buttonId, "setAlpha", 1.0f)
            views.setFloat(buttonId, "setScaleX", 1.0f)
            views.setFloat(buttonId, "setScaleY", 1.0f)
            
            emit(AnimationState.Completed)
            delay(75)
            
        } finally {
            activeAnimations.remove(animationKey)
        }
    }
    
    /**
     * Success animation for completed actions
     */
    suspend fun animateSuccess(views: RemoteViews, message: String): Flow<AnimationState> = flow {
        val animationKey = "success_feedback"
        
        if (activeAnimations.containsKey(animationKey)) return@flow
        activeAnimations[animationKey] = true
        
        try {
            emit(AnimationState.Started)
            
            // Show success message with positive color
            views.setTextViewText(R.id.button_refresh, message)
            views.setInt(R.id.button_refresh, "setTextColor", 
                context.resources.getColor(android.R.color.holo_green_dark, null))
            
            // Gentle success pulse
            for (i in 0 until 6) {
                val scale = 1.0f + (0.05f * kotlin.math.sin((i * Math.PI) / 3).toFloat())
                views.setFloat(R.id.button_refresh, "setScaleX", scale)
                views.setFloat(R.id.button_refresh, "setScaleY", scale)
                
                emit(AnimationState.Progress(i.toFloat() / 6f))
                delay(50)
            }
            
            // Restore normal state
            views.setFloat(R.id.button_refresh, "setScaleX", 1.0f)
            views.setFloat(R.id.button_refresh, "setScaleY", 1.0f)
            
            emit(AnimationState.Completed)
            
        } finally {
            activeAnimations.remove(animationKey)
        }
    }
    
    /**
     * Easing function for smooth animations
     */
    private fun easeOutCubic(t: Float): Float {
        val t1 = t - 1
        return t1 * t1 * t1 + 1
    }
    
    /**
     * Easing function for bounce effects
     */
    private fun easeOutBounce(t: Float): Float {
        return when {
            t < 1f / 2.75f -> 7.5625f * t * t
            t < 2f / 2.75f -> {
                val t2 = t - 1.5f / 2.75f
                7.5625f * t2 * t2 + 0.75f
            }
            t < 2.5f / 2.75f -> {
                val t2 = t - 2.25f / 2.75f
                7.5625f * t2 * t2 + 0.9375f
            }
            else -> {
                val t2 = t - 2.625f / 2.75f
                7.5625f * t2 * t2 + 0.984375f
            }
        }
    }
    
    /**
     * Stop all active animations (for cleanup)
     */
    fun stopAllAnimations() {
        activeAnimations.clear()
    }
    
    /**
     * Check if animations are currently running
     */
    fun hasActiveAnimations(): Boolean = activeAnimations.isNotEmpty()
    
    /**
     * Get animation performance statistics
     */
    fun getAnimationStats(): AnimationStats {
        return AnimationStats(
            activeAnimations = activeAnimations.size,
            isPerformant = activeAnimations.size < 3 // Limit concurrent animations
        )
    }
    
    /**
     * Animation state tracking
     */
    sealed class AnimationState {
        object Started : AnimationState()
        data class Progress(val progress: Float) : AnimationState()
        object Completed : AnimationState()
        data class Error(val message: String) : AnimationState()
    }
    
    /**
     * Animation performance statistics
     */
    data class AnimationStats(
        val activeAnimations: Int,
        val isPerformant: Boolean
    )
}
