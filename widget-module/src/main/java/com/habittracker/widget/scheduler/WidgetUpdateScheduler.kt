package com.habittracker.widget.scheduler

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Professional smart update scheduling system for widget operations.
 * 
 * Features:
 * - Predictive updates that anticipate user actions
 * - Optimistic UI for immediate visual feedback
 * - Batch operations for efficient processing
 * - Intelligent change detection
 * - Priority-based update scheduling
 * 
 * Performance Targets:
 * - Update detection: <50ms
 * - Batch processing: <200ms
 * - Priority scheduling efficiency: >95%
 * - Predictive accuracy: >80%
 */
class WidgetUpdateScheduler private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetUpdateScheduler? = null
        
        fun getInstance(context: Context): WidgetUpdateScheduler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetUpdateScheduler(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Scheduling constants
        private const val HIGH_PRIORITY_DELAY_MS = 0L
        private const val NORMAL_PRIORITY_DELAY_MS = 100L
        private const val LOW_PRIORITY_DELAY_MS = 500L
        private const val BATCH_WINDOW_MS = 200L
        private const val PREDICTIVE_WINDOW_MS = 2000L
        private const val MAX_CONCURRENT_UPDATES = 3
    }
    
    // Update scheduling infrastructure
    private val updateScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.Default.limitedParallelism(MAX_CONCURRENT_UPDATES) +
        CoroutineName("WidgetUpdateScheduler")
    )
    
    private val pendingUpdates = PriorityBlockingQueue<ScheduledUpdate>(
        11, // Initial capacity
        compareBy { it.priority.value }
    )
    private val batchedUpdates = mutableListOf<ScheduledUpdate>()
    private val activeUpdates = ConcurrentHashMap<String, UpdateJob>()
    
    // Scheduling state
    private val isProcessing = AtomicBoolean(false)
    private val lastUpdateTime = AtomicLong(0)
    private val updateCounter = AtomicLong(0)
    
    // Predictive analytics
    private val userActionHistory = mutableListOf<UserAction>()
    private val predictionCache = ConcurrentHashMap<String, PredictedUpdate>()
    
    /**
     * Initialize the scheduler with system setup
     */
    fun initialize(context: Context) {
        // Start the update processing loop
        updateScope.launch {
            processUpdates()
        }
        
        // Clean expired predictions periodically
        updateScope.launch {
            while (isActive) {
                delay(60000) // Clean every minute
                cleanExpiredPredictions()
            }
        }
    }
    
    /**
     * Convenience method for simple schedule updates (used by HabitsWidgetProvider)
     */
    suspend fun scheduleUpdate(
        operationName: String,
        priority: UpdatePriority,
        operation: suspend () -> Unit
    ): UpdateResult {
        return scheduleUpdate(
            updateId = "${operationName}_${System.currentTimeMillis()}",
            priority = priority,
            updateAction = operation
        )
    }
    
    /**
     * Clean expired predictions from cache
     */
    private fun cleanExpiredPredictions() {
        val currentTime = System.currentTimeMillis()
        predictionCache.values.removeAll { prediction ->
            currentTime - prediction.scheduledTime > PREDICTIVE_WINDOW_MS * 2
        }
    }
    
    /**
     * Schedule widget update with intelligent priority management
     */
    suspend fun scheduleUpdate(
        updateId: String,
        priority: UpdatePriority,
        updateAction: suspend () -> Unit,
        changeDetector: (suspend () -> Boolean)? = null,
        optimisticUpdate: (suspend () -> Unit)? = null
    ): UpdateResult = withContext(updateScope.coroutineContext) {
        
        try {
            // Check if update is already scheduled or running
            if (activeUpdates.containsKey(updateId)) {
                return@withContext UpdateResult.Duplicate("Update $updateId already scheduled")
            }
            
            // Apply optimistic update immediately for critical operations
            if (priority == UpdatePriority.CRITICAL && optimisticUpdate != null) {
                try {
                    optimisticUpdate()
                } catch (e: Exception) {
                    android.util.Log.w("WidgetScheduler", "Optimistic update failed: $updateId", e)
                }
            }
            
            // Check if update is actually needed
            if (changeDetector != null) {
                val hasChanges = changeDetector()
                if (!hasChanges) {
                    return@withContext UpdateResult.Skipped("No changes detected for $updateId")
                }
            }
            
            // Create scheduled update
            val scheduledUpdate = ScheduledUpdate(
                id = updateId,
                priority = priority,
                action = updateAction,
                scheduledTime = System.currentTimeMillis() + priority.delayMs,
                optimisticUpdate = optimisticUpdate
            )
            
            // Add to priority queue
            synchronized(pendingUpdates) {
                pendingUpdates.offer(scheduledUpdate)
            }
            
            // Start processing if not already running
            if (!isProcessing.get()) {
                processUpdates()
            }
            
            UpdateResult.Scheduled(updateId, priority)
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetScheduler", "Failed to schedule update: $updateId", e)
            UpdateResult.Failed(e.message ?: "Scheduling failed")
        }
    }
    
    /**
     * Schedule predictive update based on user behavior patterns
     */
    suspend fun schedulePredictiveUpdate(
        context: String,
        likelyAction: suspend () -> Unit
    ): PredictiveResult = withContext(updateScope.coroutineContext) {
        
        try {
            // Analyze user behavior to predict likelihood
            val prediction = analyzeUserBehavior(context)
            
            if (prediction.confidence > 0.7f) {
                // High confidence - schedule preemptive update
                val updateId = "predictive_${context}_${System.currentTimeMillis()}"
                
                val predictedUpdate = PredictedUpdate(
                    id = updateId,
                    context = context,
                    action = likelyAction,
                    confidence = prediction.confidence,
                    scheduledTime = System.currentTimeMillis() + PREDICTIVE_WINDOW_MS
                )
                
                predictionCache[updateId] = predictedUpdate
                
                // Schedule with low priority initially
                scheduleUpdate(
                    updateId = updateId,
                    priority = UpdatePriority.LOW,
                    updateAction = likelyAction
                )
                
                PredictiveResult.Scheduled(prediction.confidence)
                
            } else {
                PredictiveResult.Skipped(prediction.confidence)
            }
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetScheduler", "Predictive scheduling failed: $context", e)
            PredictiveResult.Failed(e.message ?: "Prediction failed")
        }
    }
    
    /**
     * Batch multiple updates for efficient processing
     */
    suspend fun batchUpdates(
        updates: List<BatchableUpdate>
    ): BatchResult = withContext(updateScope.coroutineContext) {
        
        try {
            val batchId = "batch_${System.currentTimeMillis()}"
            var successCount = 0
            var failureCount = 0
            val results = mutableMapOf<String, String>()
            
            // Group updates by priority
            val groupedUpdates = updates.groupBy { it.priority }
            
            // Process each priority group
            for ((priority, groupUpdates) in groupedUpdates.entries.sortedBy { it.key.value }) {
                
                // Create batch jobs for concurrent execution within priority level
                val jobs = groupUpdates.map { update ->
                    async {
                        try {
                            update.action()
                            successCount++
                            results[update.id] = "Success"
                        } catch (e: Exception) {
                            failureCount++
                            results[update.id] = "Failed: ${e.message}"
                            android.util.Log.w("WidgetScheduler", 
                                "Batch update failed: ${update.id}", e)
                        }
                    }
                }
                
                // Wait for all jobs in this priority group to complete
                jobs.awaitAll()
                
                // Small delay between priority groups
                if (priority != UpdatePriority.CRITICAL) {
                    delay(50)
                }
            }
            
            BatchResult.Completed(
                batchId = batchId,
                totalUpdates = updates.size,
                successCount = successCount,
                failureCount = failureCount,
                results = results
            )
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetScheduler", "Batch processing failed", e)
            BatchResult.Failed(e.message ?: "Batch processing failed")
        }
    }
    
    /**
     * Intelligent change detection to avoid unnecessary updates
     */
    suspend fun <T> detectChanges(
        key: String,
        currentValue: T,
        changeThreshold: Float = 0.1f
    ): ChangeResult<T> = withContext(updateScope.coroutineContext) {
        
        try {
            val lastValue = changeCache[key]
            
            if (lastValue == null) {
                // First time - always consider as changed
                changeCache[key] = ChangeRecord(currentValue as Any, System.currentTimeMillis())
                return@withContext ChangeResult.Changed(currentValue, null)
            }
            
            val hasChanged = when (currentValue) {
                is Number -> {
                    val current = currentValue.toDouble()
                    val previous = (lastValue.value as? Number)?.toDouble() ?: 0.0
                    kotlin.math.abs(current - previous) / kotlin.math.max(kotlin.math.abs(previous), 1.0) > changeThreshold
                }
                is String -> currentValue != lastValue.value
                is List<*> -> currentValue.size != (lastValue.value as? List<*>)?.size ||
                            currentValue != lastValue.value
                else -> currentValue != lastValue.value
            }
            
            if (hasChanged) {
                changeCache[key] = ChangeRecord(currentValue as Any, System.currentTimeMillis())
                ChangeResult.Changed(currentValue, lastValue.value as? T)
            } else {
                ChangeResult.Unchanged(currentValue)
            }
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetScheduler", "Change detection failed for key: $key", e)
            ChangeResult.Error(e.message ?: "Change detection failed")
        }
    }
    
    /**
     * Priority-based update processing with smart queuing
     */
    private suspend fun processUpdates() = withContext(updateScope.coroutineContext) {
        if (!isProcessing.compareAndSet(false, true)) {
            return@withContext // Already processing
        }
        
        try {
            while (true) {
                val update = synchronized(pendingUpdates) {
                    pendingUpdates.poll()
                } ?: break
                
                // Check if it's time to execute this update
                val currentTime = System.currentTimeMillis()
                if (currentTime < update.scheduledTime) {
                    // Put it back and wait
                    synchronized(pendingUpdates) {
                        pendingUpdates.offer(update)
                    }
                    delay(update.scheduledTime - currentTime)
                    continue
                }
                
                // Execute the update
                executeUpdate(update)
                
                // Track execution for analytics
                updateCounter.incrementAndGet()
                lastUpdateTime.set(currentTime)
            }
            
        } finally {
            isProcessing.set(false)
        }
    }
    
    /**
     * Execute individual update with error handling
     */
    private suspend fun executeUpdate(update: ScheduledUpdate) {
        val job = UpdateJob(
            id = update.id,
            startTime = System.currentTimeMillis(),
            job = updateScope.launch {
                try {
                    update.action()
                    activeUpdates.remove(update.id)
                } catch (e: Exception) {
                    android.util.Log.e("WidgetScheduler", "Update execution failed: ${update.id}", e)
                    activeUpdates.remove(update.id)
                }
            }
        )
        
        activeUpdates[update.id] = job
    }
    
    /**
     * Analyze user behavior for predictive updates
     */
    private fun analyzeUserBehavior(context: String): BehaviorPrediction {
        val recentActions = userActionHistory
            .filter { it.context == context }
            .filter { System.currentTimeMillis() - it.timestamp < 300000 } // Last 5 minutes
        
        if (recentActions.isEmpty()) {
            return BehaviorPrediction(confidence = 0.2f, pattern = "No recent activity")
        }
        
        // Simple frequency-based prediction
        val actionFrequency = recentActions.size / 5.0f // Actions per minute
        val confidence = minOf(actionFrequency / 2.0f, 0.9f) // Cap at 90%
        
        return BehaviorPrediction(
            confidence = confidence,
            pattern = "Frequent ${context} activity: ${recentActions.size} actions"
        )
    }
    
    /**
     * Record user action for behavioral analysis
     */
    fun recordUserAction(context: String, action: String) {
        val userAction = UserAction(
            context = context,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        
        userActionHistory.add(userAction)
        
        // Keep only recent history (last hour)
        val cutoffTime = System.currentTimeMillis() - 3600000
        userActionHistory.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * Cancel scheduled update
     */
    suspend fun cancelUpdate(updateId: String): Boolean {
        activeUpdates[updateId]?.job?.cancel()
        activeUpdates.remove(updateId)
        
        synchronized(pendingUpdates) {
            return pendingUpdates.removeIf { it.id == updateId }
        }
    }
    
    /**
     * Get comprehensive scheduling statistics
     */
    fun getSchedulingStats(): SchedulingStats {
        return SchedulingStats(
            totalUpdates = updateCounter.get(),
            activeUpdates = activeUpdates.size,
            pendingUpdates = pendingUpdates.size,
            lastUpdateTime = lastUpdateTime.get(),
            isProcessing = isProcessing.get(),
            predictionCacheSize = predictionCache.size,
            userActionHistory = userActionHistory.size
        )
    }
    
    /**
     * Shutdown scheduler and cleanup resources
     */
    fun shutdown() {
        updateScope.cancel()
        activeUpdates.clear()
        pendingUpdates.clear()
        predictionCache.clear()
        userActionHistory.clear()
    }
    
    // Change tracking cache
    private val changeCache = ConcurrentHashMap<String, ChangeRecord<Any>>()
    
    /**
     * Update priority levels
     */
    enum class UpdatePriority(val value: Int, val delayMs: Long) {
        CRITICAL(0, HIGH_PRIORITY_DELAY_MS),    // User interactions
        HIGH(1, HIGH_PRIORITY_DELAY_MS),        // Important updates
        MEDIUM(2, NORMAL_PRIORITY_DELAY_MS),    // Medium priority updates
        NORMAL(3, NORMAL_PRIORITY_DELAY_MS),    // Regular updates
        LOW(4, LOW_PRIORITY_DELAY_MS)           // Background updates
    }
    
    /**
     * Scheduled update container
     */
    private data class ScheduledUpdate(
        val id: String,
        val priority: UpdatePriority,
        val action: suspend () -> Unit,
        val scheduledTime: Long,
        val optimisticUpdate: (suspend () -> Unit)? = null
    )
    
    /**
     * Update job tracking
     */
    private data class UpdateJob(
        val id: String,
        val startTime: Long,
        val job: Job
    )
    
    /**
     * Batchable update definition
     */
    data class BatchableUpdate(
        val id: String,
        val priority: UpdatePriority,
        val action: suspend () -> Unit
    )
    
    /**
     * Predicted update container
     */
    private data class PredictedUpdate(
        val id: String,
        val context: String,
        val action: suspend () -> Unit,
        val confidence: Float,
        val scheduledTime: Long
    )
    
    /**
     * User action tracking
     */
    private data class UserAction(
        val context: String,
        val action: String,
        val timestamp: Long
    )
    
    /**
     * Change record for detection
     */
    private data class ChangeRecord<T>(
        val value: T,
        val timestamp: Long
    )
    
    /**
     * Behavior prediction result
     */
    private data class BehaviorPrediction(
        val confidence: Float,
        val pattern: String
    )
    
    /**
     * Update scheduling results
     */
    sealed class UpdateResult {
        data class Scheduled(val updateId: String, val priority: UpdatePriority) : UpdateResult()
        data class Duplicate(val message: String) : UpdateResult()
        data class Skipped(val reason: String) : UpdateResult()
        data class Failed(val error: String) : UpdateResult()
    }
    
    /**
     * Predictive update results
     */
    sealed class PredictiveResult {
        data class Scheduled(val confidence: Float) : PredictiveResult()
        data class Skipped(val confidence: Float) : PredictiveResult()
        data class Failed(val error: String) : PredictiveResult()
    }
    
    /**
     * Batch processing results
     */
    sealed class BatchResult {
        data class Completed(
            val batchId: String,
            val totalUpdates: Int,
            val successCount: Int,
            val failureCount: Int,
            val results: Map<String, String>
        ) : BatchResult()
        data class Failed(val error: String) : BatchResult()
    }
    
    /**
     * Change detection results
     */
    sealed class ChangeResult<T> {
        data class Changed<T>(val current: T, val previous: T?) : ChangeResult<T>()
        data class Unchanged<T>(val value: T) : ChangeResult<T>()
        data class Error<T>(val message: String) : ChangeResult<T>()
    }
    
    /**
     * Scheduling performance statistics
     */
    data class SchedulingStats(
        val totalUpdates: Long,
        val activeUpdates: Int,
        val pendingUpdates: Int,
        val lastUpdateTime: Long,
        val isProcessing: Boolean,
        val predictionCacheSize: Int,
        val userActionHistory: Int
    ) {
        val isHealthy: Boolean
            get() = activeUpdates < MAX_CONCURRENT_UPDATES && 
                   pendingUpdates < 20 &&
                   System.currentTimeMillis() - lastUpdateTime < 30000
    }
}
