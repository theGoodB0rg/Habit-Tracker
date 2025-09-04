package com.habittracker.ui.design

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern Design System following Material 3 and industry best practices
 * 
 * Features:
 * - Responsive spacing system
 * - Professional elevation tokens
 * - Consistent shape language
 * - Accessibility-compliant touch targets
 */
object DesignTokens {
    
    /**
     * Responsive Spacing System
     * Based on 4dp grid with responsive scaling
     */
    object Spacing {
        // Base unit - all spacing should be multiples of this
        val baseline: Dp = 4.dp
        
        // Semantic spacing tokens
        val tiny: Dp = 4.dp      // For internal component spacing
        val small: Dp = 8.dp     // For small gaps
        val medium: Dp = 16.dp   // For standard component spacing
        val large: Dp = 24.dp    // For section spacing
        val extraLarge: Dp = 32.dp // For major layout spacing
        val huge: Dp = 48.dp     // For hero sections
        
        // Component-specific spacing
        val cardPadding: Dp = 16.dp
        val screenPadding: Dp = 16.dp
        val sectionSpacing: Dp = 24.dp
        val itemSpacing: Dp = 12.dp
        
        // Responsive padding values
        val responsivePaddingValues = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        )
    }
    
    /**
     * Professional Elevation System
     * Following Material 3 elevation guidelines
     */
    object Elevation {
        val none: Dp = 0.dp
        val low: Dp = 1.dp       // For subtle separation
        val medium: Dp = 3.dp    // For cards and surfaces
        val high: Dp = 6.dp      // For floating elements
        val highest: Dp = 12.dp  // For modals and overlays
        
        // Semantic elevation tokens
        val card: Dp = medium
        val fab: Dp = high
        val modal: Dp = highest
        val tooltip: Dp = high
    }
    
    /**
     * Modern Shape Language
     * Consistent corner radius system
     */
    object Shapes {
        val tiny = RoundedCornerShape(4.dp)
        val small = RoundedCornerShape(8.dp)
        val medium = RoundedCornerShape(12.dp)
        val large = RoundedCornerShape(16.dp)
        val extraLarge = RoundedCornerShape(24.dp)
        
        // Component-specific shapes
        val card = medium
        val button = small
        val fab = RoundedCornerShape(16.dp)
        val modal = large
    }
    
    /**
     * Touch Target Guidelines
     * WCAG 2.1 AAA compliant minimum sizes
     */
    object TouchTargets {
        val minimum: Dp = 44.dp    // iOS HIG minimum
        val recommended: Dp = 48.dp // Android Material minimum
        val comfortable: Dp = 56.dp // For primary actions
        
        // Icon button sizes
        val iconButtonSmall: Dp = 40.dp
        val iconButtonMedium: Dp = 48.dp
        val iconButtonLarge: Dp = 56.dp
    }
    
    /**
     * Responsive Breakpoints
     * For adaptive layouts
     */
    object Breakpoints {
        val compact: Dp = 600.dp
        val medium: Dp = 840.dp
        val expanded: Dp = 1200.dp
    }
    
    /**
     * Animation Specifications
     * Consistent motion design
     */
    object Motion {
        const val durationShort = 150
        const val durationMedium = 300
        const val durationLong = 500
        
        // Semantic duration tokens
        const val quickTransition = durationShort
        const val standardTransition = durationMedium
        const val emphasizedTransition = durationLong
    }
}
