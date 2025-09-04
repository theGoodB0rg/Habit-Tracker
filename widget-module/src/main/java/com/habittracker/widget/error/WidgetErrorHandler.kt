package com.habittracker.widget.error

import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import com.habittracker.widget.R

/**
 * Professional error handling system for widget operations.
 * 
 * Features:
 * - Automatic retry with exponential backoff
 * - Graceful fallback strategies
 * - User-friendly error messaging
 * - Comprehensive logging and diagnostics
 * - Crash prevention with defensive programming
 * 
 * Reliability Targets:
 * - 99.9% uptime
 * - <1 second recovery time
 * - Automatic error resolution
 * - Zero crashes from widget operations
 */
class WidgetErrorHandler private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetErrorHandler? = null
        
        fun getInstance(context: Context): WidgetErrorHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetErrorHandler(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Error handling constants
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 8000L
        private const val ERROR_DISPLAY_DURATION_MS = 3000L
        private const val CIRCUIT_BREAKER_THRESHOLD = 5
        private const val CIRCUIT_BREAKER_TIMEOUT_MS = 30000L
    }
    
    // Error tracking and monitoring
    private val errorCount = AtomicInteger(0)
    private val lastErrorTime = AtomicLong(0)
    private val circuitBreakerCount = AtomicInteger(0)
    private val lastCircuitBreakerTime = AtomicLong(0)
    
    // Error categories for different handling strategies
    enum class ErrorCategory {
        NETWORK,          // Database connection issues
        MEMORY,           // Out of memory, resource constraints
        PERMISSION,       // Permission denied operations
        DATA_CORRUPTION,  // Invalid data states
        TIMEOUT,          // Operation timeout
        UNKNOWN           // Unclassified errors
    }
    
    // Error severity levels
    enum class ErrorSeverity {
        LOW,      // Minor issues, automatic recovery
        MEDIUM,   // Visible issues, user notification
        HIGH,     // Service degradation, immediate attention
        CRITICAL  // Service failure, emergency handling
    }
    
    /**
     * Execute operation with comprehensive error handling and retry logic
     */
    suspend fun <T> withErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        fallback: (suspend () -> T)? = null
    ): ErrorResult<T> {
        
        // Check circuit breaker state
        if (isCircuitBreakerOpen()) {
            return handleCircuitBreakerOpen(operationName, fallback)
        }
        
        var lastException: Exception? = null
        
        for (attempt in 0 until maxRetries) {
            try {
                val result = operation()
                
                // Reset error counters on success
                if (attempt > 0) {
                    resetErrorCounters()
                    logRecovery(operationName, attempt)
                }
                
                return ErrorResult.Success(result)
                
            } catch (e: Exception) {
                lastException = e
                
                val errorCategory = categorizeError(e)
                val severity = determineSeverity(e, attempt)
                
                logError(operationName, e, attempt, errorCategory, severity)
                
                // Increment error tracking
                errorCount.incrementAndGet()
                lastErrorTime.set(System.currentTimeMillis())
                
                // Check if we should continue retrying
                if (attempt < maxRetries - 1) {
                    val shouldRetry = shouldRetry(e, attempt, errorCategory)
                    if (shouldRetry) {
                        val delayMs = calculateRetryDelay(attempt)
                        android.util.Log.d("WidgetErrorHandler", 
                            "Retrying $operationName in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                        delay(delayMs)
                        continue
                    }
                }
                
                // Update circuit breaker if too many failures
                updateCircuitBreaker()
                break
            }
        }
        
        // All retries failed, try fallback
        return handleOperationFailure(lastException!!, operationName, fallback)
    }
    
    /**
     * Create error view for widget when regular operation fails
     */
    fun createErrorView(context: Context, error: Exception): RemoteViews {
        val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        
        val errorMessage = when (error) {
            is java.net.UnknownHostException -> "No internet connection"
            is java.net.SocketTimeoutException -> "Connection timeout"
            is OutOfMemoryError -> "Memory error"
            is SecurityException -> "Permission denied"
            else -> "Error loading widget"
        }
        
        views.setTextViewText(android.R.id.text1, errorMessage)
        return views
    }
    
    /**
     * Handle UI error states with user-friendly messaging
     */
    suspend fun handleUIError(
        views: RemoteViews,
        error: Exception,
        context: String
    ): Flow<ErrorUIState> = flow {
        emit(ErrorUIState.Showing)
        
        try {
            val userMessage = generateUserFriendlyMessage(error, context)
            val errorCategory = categorizeError(error)
            
            // Show appropriate error state based on category
            when (errorCategory) {
                ErrorCategory.NETWORK -> {
                    views.setTextViewText(R.id.button_refresh, "Network Error")
                }
                
                ErrorCategory.MEMORY -> {
                    views.setTextViewText(R.id.widget_title, "Memory Error")
                }
                
                ErrorCategory.DATA_CORRUPTION -> {
                    views.setTextViewText(R.id.daily_progress, "Data Error")
                }
                
                else -> {
                    views.setTextViewText(R.id.button_refresh, userMessage)
                }
            }
            
            emit(ErrorUIState.Displayed(userMessage))
            
            // Auto-hide error after duration
            delay(ERROR_DISPLAY_DURATION_MS)
            
            // Restore normal UI state
            restoreNormalUIState(views)
            emit(ErrorUIState.Hidden)
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetErrorHandler", "Failed to handle UI error", e)
            emit(ErrorUIState.Failed(e.message ?: "Error handling failed"))
        }
    }
    
    /**
     * Generate recovery suggestions for different error types
     */
    fun generateRecoveryAction(error: Exception): RecoveryAction {
        val category = categorizeError(error)
        
        return when (category) {
            ErrorCategory.NETWORK -> RecoveryAction(
                message = "Retry Connection",
                action = RecoveryAction.Action.RETRY,
                autoRetry = true,
                userActionRequired = false
            )
            
            ErrorCategory.MEMORY -> RecoveryAction(
                message = "Free Memory",
                action = RecoveryAction.Action.REDUCE_LOAD,
                autoRetry = true,
                userActionRequired = false
            )
            
            ErrorCategory.PERMISSION -> RecoveryAction(
                message = "Grant Permission",
                action = RecoveryAction.Action.REQUEST_PERMISSION,
                autoRetry = false,
                userActionRequired = true
            )
            
            ErrorCategory.DATA_CORRUPTION -> RecoveryAction(
                message = "Reload Data",
                action = RecoveryAction.Action.RESET_DATA,
                autoRetry = false,
                userActionRequired = true
            )
            
            ErrorCategory.TIMEOUT -> RecoveryAction(
                message = "Retry Operation",
                action = RecoveryAction.Action.RETRY,
                autoRetry = true,
                userActionRequired = false
            )
            
            ErrorCategory.UNKNOWN -> RecoveryAction(
                message = "Try Again",
                action = RecoveryAction.Action.RETRY,
                autoRetry = true,
                userActionRequired = false
            )
        }
    }
    
    /**
     * Categorize errors for appropriate handling
     */
    private fun categorizeError(error: Exception): ErrorCategory {
        return when {
            error is java.sql.SQLException || 
            error.message?.contains("database", ignoreCase = true) == true -> ErrorCategory.NETWORK
            
            error is OutOfMemoryError ||
            error.message?.contains("memory", ignoreCase = true) == true -> ErrorCategory.MEMORY
            
            error is SecurityException ||
            error.message?.contains("permission", ignoreCase = true) == true -> ErrorCategory.PERMISSION
            
            error.message?.contains("corrupt", ignoreCase = true) == true ||
            error.message?.contains("invalid", ignoreCase = true) == true -> ErrorCategory.DATA_CORRUPTION
            
            error is java.util.concurrent.TimeoutException ||
            error.message?.contains("timeout", ignoreCase = true) == true -> ErrorCategory.TIMEOUT
            
            else -> ErrorCategory.UNKNOWN
        }
    }
    
    /**
     * Determine error severity based on context and attempt
     */
    private fun determineSeverity(error: Exception, attempt: Int): ErrorSeverity {
        return when {
            error is OutOfMemoryError -> ErrorSeverity.CRITICAL
            error is SecurityException -> ErrorSeverity.HIGH
            attempt >= MAX_RETRY_ATTEMPTS - 1 -> ErrorSeverity.HIGH
            categorizeError(error) == ErrorCategory.DATA_CORRUPTION -> ErrorSeverity.HIGH
            else -> ErrorSeverity.MEDIUM
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val delay = BASE_RETRY_DELAY_MS * (1L shl attempt) // 2^attempt
        return minOf(delay, MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Determine if operation should be retried
     */
    private fun shouldRetry(error: Exception, attempt: Int, category: ErrorCategory): Boolean {
        // Don't retry certain error types
        if (category == ErrorCategory.PERMISSION) return false
        if (error is SecurityException) return false
        if (error is OutOfMemoryError && attempt > 0) return false
        
        return true
    }
    
    /**
     * Handle circuit breaker logic
     */
    private fun isCircuitBreakerOpen(): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastFailure = now - lastCircuitBreakerTime.get()
        
        // Reset circuit breaker after timeout
        if (timeSinceLastFailure > CIRCUIT_BREAKER_TIMEOUT_MS) {
            circuitBreakerCount.set(0)
            return false
        }
        
        return circuitBreakerCount.get() >= CIRCUIT_BREAKER_THRESHOLD
    }
    
    /**
     * Update circuit breaker state
     */
    private fun updateCircuitBreaker() {
        circuitBreakerCount.incrementAndGet()
        lastCircuitBreakerTime.set(System.currentTimeMillis())
        
        if (circuitBreakerCount.get() >= CIRCUIT_BREAKER_THRESHOLD) {
            android.util.Log.w("WidgetErrorHandler", "Circuit breaker opened due to repeated failures")
        }
    }
    
    /**
     * Handle circuit breaker open state
     */
    private suspend fun <T> handleCircuitBreakerOpen(
        operationName: String,
        fallback: (suspend () -> T)?
    ): ErrorResult<T> {
        android.util.Log.w("WidgetErrorHandler", "Circuit breaker open for $operationName")
        
        return if (fallback != null) {
            try {
                val fallbackResult = fallback()
                ErrorResult.Fallback(fallbackResult, "Circuit breaker open - using fallback")
            } catch (e: Exception) {
                ErrorResult.Failure(e, "Both primary and fallback operations failed")
            }
        } else {
            ErrorResult.Failure(
                Exception("Circuit breaker open - service temporarily unavailable"),
                "Service temporarily unavailable"
            )
        }
    }
    
    /**
     * Handle operation failure with fallback
     */
    private suspend fun <T> handleOperationFailure(
        error: Exception,
        operationName: String,
        fallback: (suspend () -> T)?
    ): ErrorResult<T> {
        
        if (fallback != null) {
            try {
                android.util.Log.d("WidgetErrorHandler", "Attempting fallback for $operationName")
                val fallbackResult = fallback()
                return ErrorResult.Fallback(fallbackResult, "Primary operation failed - using fallback")
            } catch (fallbackError: Exception) {
                android.util.Log.e("WidgetErrorHandler", "Fallback also failed for $operationName", fallbackError)
            }
        }
        
        return ErrorResult.Failure(error, "Operation failed after all retry attempts")
    }
    
    /**
     * Generate user-friendly error messages
     */
    private fun generateUserFriendlyMessage(error: Exception, context: String): String {
        val category = categorizeError(error)
        
        return when (category) {
            ErrorCategory.NETWORK -> "Connection issue. Tap to retry."
            ErrorCategory.MEMORY -> "Low memory. Optimizing..."
            ErrorCategory.PERMISSION -> "Permission needed. Check settings."
            ErrorCategory.DATA_CORRUPTION -> "Data issue. Syncing..."
            ErrorCategory.TIMEOUT -> "Loading timeout. Retrying..."
            ErrorCategory.UNKNOWN -> "Temporary issue. Retrying..."
        }
    }
    
    /**
     * Restore normal UI state after error
     */
    private fun restoreNormalUIState(views: RemoteViews) {
        try {
            // Restore refresh button
            views.setTextViewText(R.id.button_refresh, 
                "Refresh")
            views.setInt(R.id.button_refresh, "setTextColor", 
                context.resources.getColor(R.color.widgetTextColor, null))
            
            // Restore title color
            views.setInt(R.id.widget_title, "setTextColor", 
                context.resources.getColor(R.color.widgetTextColor, null))
            
            // Restore progress color
            views.setInt(R.id.daily_progress, "setTextColor", 
                context.resources.getColor(R.color.widgetTextColor, null))
                
        } catch (e: Exception) {
            android.util.Log.w("WidgetErrorHandler", "Failed to restore normal UI state", e)
        }
    }
    
    /**
     * Reset error counters after successful operation
     */
    private fun resetErrorCounters() {
        errorCount.set(0)
        if (circuitBreakerCount.get() > 0) {
            circuitBreakerCount.decrementAndGet()
        }
    }
    
    /**
     * Log error with comprehensive details
     */
    private fun logError(
        operationName: String,
        error: Exception,
        attempt: Int,
        category: ErrorCategory,
        severity: ErrorSeverity
    ) {
        val logLevel = when (severity) {
            ErrorSeverity.LOW -> android.util.Log.DEBUG
            ErrorSeverity.MEDIUM -> android.util.Log.WARN
            ErrorSeverity.HIGH, ErrorSeverity.CRITICAL -> android.util.Log.ERROR
        }
        
        val message = "Widget operation failed: $operationName (attempt ${attempt + 1}) " +
                     "Category: $category, Severity: $severity"
        
        android.util.Log.println(logLevel, "WidgetErrorHandler", "$message - ${error.message}")
    }
    
    /**
     * Log successful recovery
     */
    private fun logRecovery(operationName: String, attempts: Int) {
        android.util.Log.i("WidgetErrorHandler", 
            "Widget operation recovered: $operationName after $attempts attempts")
    }
    
    /**
     * Get comprehensive error statistics
     */
    fun getErrorStats(): ErrorStats {
        val now = System.currentTimeMillis()
        val timeSinceLastError = now - lastErrorTime.get()
        
        return ErrorStats(
            totalErrors = errorCount.get(),
            circuitBreakerCount = circuitBreakerCount.get(),
            timeSinceLastError = timeSinceLastError,
            isCircuitBreakerOpen = isCircuitBreakerOpen(),
            healthStatus = calculateHealthStatus()
        )
    }
    
    /**
     * Calculate overall health status
     */
    private fun calculateHealthStatus(): HealthStatus {
        val errors = errorCount.get()
        val isCircuitOpen = isCircuitBreakerOpen()
        val timeSinceError = System.currentTimeMillis() - lastErrorTime.get()
        
        return when {
            isCircuitOpen -> HealthStatus.CRITICAL
            errors > 10 && timeSinceError < 60000 -> HealthStatus.POOR
            errors > 5 && timeSinceError < 30000 -> HealthStatus.FAIR
            errors == 0 || timeSinceError > 300000 -> HealthStatus.EXCELLENT
            else -> HealthStatus.GOOD
        }
    }
    
    /**
     * Error handling result wrapper
     */
    sealed class ErrorResult<out T> {
        data class Success<T>(val data: T) : ErrorResult<T>()
        data class Fallback<T>(val data: T, val reason: String) : ErrorResult<T>()
        data class Failure<T>(val error: Exception, val message: String) : ErrorResult<T>()
        
        val isSuccess: Boolean get() = this is Success
        val isFallback: Boolean get() = this is Fallback
        val isFailure: Boolean get() = this is Failure
        
        fun getOrNull(): T? = when (this) {
            is Success -> data
            is Fallback -> data
            is Failure -> null
        }
    }
    
    /**
     * Recovery action recommendations
     */
    data class RecoveryAction(
        val message: String,
        val action: Action,
        val autoRetry: Boolean,
        val userActionRequired: Boolean
    ) {
        enum class Action {
            RETRY,
            REDUCE_LOAD,
            REQUEST_PERMISSION,
            RESET_DATA,
            CONTACT_SUPPORT
        }
    }
    
    /**
     * Error UI states
     */
    sealed class ErrorUIState {
        object Showing : ErrorUIState()
        data class Displayed(val message: String) : ErrorUIState()
        object Hidden : ErrorUIState()
        data class Failed(val reason: String) : ErrorUIState()
    }
    
    /**
     * Error monitoring statistics
     */
    data class ErrorStats(
        val totalErrors: Int,
        val circuitBreakerCount: Int,
        val timeSinceLastError: Long,
        val isCircuitBreakerOpen: Boolean,
        val healthStatus: HealthStatus
    )
    
    /**
     * Overall system health status
     */
    enum class HealthStatus {
        EXCELLENT,  // No recent errors
        GOOD,       // Minor issues
        FAIR,       // Some issues
        POOR,       // Frequent issues
        CRITICAL    // System failure
    }
}
