package com.habittracker.ui.modifiers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Default debounce interval in milliseconds.
 * Prevents rapid clicks from causing Compose SlotTable corruption crashes.
 */
private const val DEFAULT_DEBOUNCE_INTERVAL_MS = 400L

/**
 * A debounced clickable modifier that prevents rapid consecutive clicks
 * from triggering multiple actions. This helps avoid Compose runtime crashes
 * (ArrayIndexOutOfBoundsException in SlotTable) when rapid clicks cause
 * concurrent state mutations in LazyColumn items with AnimatedVisibility.
 *
 * @param debounceIntervalMs The minimum time between clicks in milliseconds
 * @param enabled Whether the clickable is enabled
 * @param onClick The click handler
 */
@Composable
fun Modifier.debouncedClickable(
    debounceIntervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return this.clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = androidx.compose.material.ripple.rememberRipple()
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceIntervalMs) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

/**
 * Creates a debounced click handler function that can be passed to onClick parameters.
 * Useful when you need to debounce clicks on IconButton or other components
 * that take onClick as a lambda parameter rather than using Modifier.clickable.
 *
 * @param debounceIntervalMs The minimum time between clicks in milliseconds
 * @param onClick The click handler to debounce
 * @return A debounced click handler
 */
@Composable
fun rememberDebouncedClick(
    debounceIntervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return remember(onClick) {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= debounceIntervalMs) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}
