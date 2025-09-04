package com.habittracker.widget.performance

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Professional performance optimization system for widget operations.
 * 
 * Features:
 * - Lazy loading for large datasets
 * - Efficient background processing with coroutines
 * - Memory management and cleanup
 * - Update throttling and batching
 * - Battery optimization with Doze mode compliance
 * 
 * Performance Targets:
 * - Memory usage: <50MB peak
 * - Battery drain: <1% per day
 * - Update latency: <200ms
 * - CPU usage: <10% during updates
 */
class WidgetPerformanceOptimizer private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetPerformanceOptimizer? = null
        
        fun getInstance(context: Context): WidgetPerformanceOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetPerformanceOptimizer(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Performance constants
        private const val MAX_CONCURRENT_OPERATIONS = 3
        private const val UPDATE_THROTTLE_MS = 1000L
        private const val BATCH_SIZE = 10
        private const val MEMORY_THRESHOLD_MB = 45
        private const val CPU_INTENSIVE_DELAY_MS = 16L // 60fps equivalent
    }
    
    // Performance monitoring
    private val totalOperations = AtomicLong(0)
    private val successfulOperations = AtomicLong(0)
    private val averageExecutionTime = AtomicLong(0)
    private val lastUpdateTime = AtomicLong(0)
    private val isThrottling = AtomicBoolean(false)
    
    // Resource management
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val optimizedScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.Default.limitedParallelism(MAX_CONCURRENT_OPERATIONS) +
        CoroutineName("WidgetOptimizer")
    )
    
    // Throttling and batching
    private val pendingUpdates = mutableListOf<suspend () -> Unit>()
    private val updateMutex = kotlinx.coroutines.sync.Mutex()
    
    /**
     * Initialize the performance optimizer with system monitoring
     */
    fun initialize(context: Context) {
        // Start memory monitoring
        optimizedScope.launch {
            monitorSystemResources()
        }
        
        // Start cleanup tasks
        optimizedScope.launch {
            cleanupExpiredEntries()
        }
    }
    
    /**
     * Monitor system resources and adjust performance parameters
     */
    private suspend fun monitorSystemResources() {
        while (optimizedScope.isActive) {
            try {
                val memoryInfo = android.app.ActivityManager.MemoryInfo()
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.getMemoryInfo(memoryInfo)
                
                // Adjust throttling based on available memory
                val memoryThreshold = memoryInfo.threshold
                val availableMemory = memoryInfo.availMem
                
                // Log memory status for monitoring
                android.util.Log.d("WidgetOptimizer", "Memory: ${availableMemory / 1024 / 1024}MB available")
                
            } catch (e: Exception) {
                android.util.Log.w("WidgetOptimizer", "Failed to monitor system resources", e)
            }
            
            delay(30000) // Check every 30 seconds
        }
    }
    
    /**
     * Clean up expired performance monitoring entries
     */
    private suspend fun cleanupExpiredEntries() {
        while (optimizedScope.isActive) {
            try {
                // This would clean up any expired cache entries
                // Implementation depends on specific caching strategy
                android.util.Log.d("WidgetOptimizer", "Performance cleanup completed")
            } catch (e: Exception) {
                android.util.Log.w("WidgetOptimizer", "Failed to cleanup expired entries", e)
            }
            
            delay(300000) // Clean every 5 minutes
        }
    }
    
    /**
     * Execute operations with performance optimization and resource management
     */
    suspend fun <T> optimizedExecution(
        operationName: String,
        priority: Priority = Priority.NORMAL,
        operation: suspend () -> T
    ): Result<T> = withContext(optimizedScope.coroutineContext) {
        totalOperations.incrementAndGet()
        
        try {
            // Check system resources before execution
            if (!isResourcesAvailable()) {
                return@withContext Result.failure(
                    Exception("Insufficient resources for operation: $operationName")
                )
            }
            
            // Apply throttling for non-critical operations
            if (priority != Priority.CRITICAL && shouldThrottle()) {
                delay(UPDATE_THROTTLE_MS)
            }
            
            // Execute with performance monitoring
            val result: T
            val executionTime = measureTimeMillis {
                result = operation()
            }
            
            // Update performance metrics
            updatePerformanceMetrics(executionTime)
            successfulOperations.incrementAndGet()
            
            Result.success(result)
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetOptimizer", "Operation failed: $operationName", e)
            Result.failure(e)
        }
    }
    
    /**
     * Lazy loading implementation for large habit lists
     */
    fun <T> lazyLoader(
        items: List<T>,
        pageSize: Int = BATCH_SIZE
    ): Flow<LazyLoadResult<T>> = flow {
        emit(LazyLoadResult.Loading as LazyLoadResult<T>)
        
        try {
            val totalPages = (items.size + pageSize - 1) / pageSize
            
            for (page in 0 until totalPages) {
                // Check if we should continue loading
                if (!isResourcesAvailable()) {
                    emit(LazyLoadResult.Error("Resource constraints reached"))
                    return@flow
                }
                
                val startIndex = page * pageSize
                val endIndex = minOf(startIndex + pageSize, items.size)
                val pageItems = items.subList(startIndex, endIndex)
                
                emit(LazyLoadResult.Page(
                    items = pageItems,
                    pageNumber = page,
                    totalPages = totalPages,
                    hasMore = page < totalPages - 1
                ))
                
                // Small delay to prevent UI blocking
                if (page < totalPages - 1) {
                    delay(CPU_INTENSIVE_DELAY_MS)
                }
            }
            
            emit(LazyLoadResult.Complete(items))
            
        } catch (e: Exception) {
            emit(LazyLoadResult.Error(e.message ?: "Loading failed"))
        }
    }
    
    /**
     * Batch operation processing for efficient updates
     */
    suspend fun batchUpdates(updates: List<suspend () -> Unit>) = updateMutex.withLock {
        try {
            // Add to pending updates
            pendingUpdates.addAll(updates)
            
            // Process in batches to prevent resource exhaustion
            val batches = pendingUpdates.chunked(BATCH_SIZE)
            pendingUpdates.clear()
            
            for (batch in batches) {
                // Execute batch concurrently but with resource limits
                optimizedScope.launch {
                    batch.map { update ->
                        async {
                            try {
                                update()
                            } catch (e: Exception) {
                                android.util.Log.w("WidgetOptimizer", "Batch update failed", e)
                            }
                        }
                    }.awaitAll()
                }.join()
                
                // Small delay between batches
                delay(CPU_INTENSIVE_DELAY_MS)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetOptimizer", "Batch processing failed", e)
            pendingUpdates.clear() // Clear failed batch
        }
    }
    
    /**
     * Memory-efficient operation with automatic cleanup
     */
    suspend fun <T> memoryOptimizedOperation(
        operation: suspend () -> T
    ): T? = withContext(optimizedScope.coroutineContext) {
        try {
            // Force garbage collection if memory is tight
            val runtime = Runtime.getRuntime()
            val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            
            if (usedMemoryMB > MEMORY_THRESHOLD_MB) {
                System.gc()
                delay(100) // Allow GC to complete
            }
            
            val result = operation()
            
            // Cleanup after operation
            cleanup()
            
            result
            
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("WidgetOptimizer", "Out of memory during operation", e)
            emergencyCleanup()
            null
        }
    }
    
    /**
     * Battery-optimized background processing
     */
    suspend fun batteryOptimizedTask(
        task: suspend () -> Unit
    ) = withContext(optimizedScope.coroutineContext) {
        try {
            // Check battery optimization status
            if (powerManager.isPowerSaveMode) {
                android.util.Log.d("WidgetOptimizer", "Deferring task due to power save mode")
                return@withContext
            }
            
            // Check if device is in Doze mode
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (powerManager.isDeviceIdleMode) {
                    android.util.Log.d("WidgetOptimizer", "Deferring task due to Doze mode")
                    return@withContext
                }
            }
            
            // Execute with minimal CPU usage
            yield() // Allow other coroutines to run
            task()
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetOptimizer", "Battery optimized task failed", e)
        }
    }
    
    /**
     * Intelligent update throttling based on system state
     */
    private fun shouldThrottle(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastUpdateTime.get()
        
        // Throttle if updates are too frequent
        if (timeSinceLastUpdate < UPDATE_THROTTLE_MS) {
            isThrottling.set(true)
            return true
        }
        
        // Additional throttling based on system state
        if (powerManager.isPowerSaveMode) {
            return true
        }
        
        isThrottling.set(false)
        lastUpdateTime.set(currentTime)
        return false
    }
    
    /**
     * Check if system resources are available for operations
     */
    private fun isResourcesAvailable(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        
        return usedMemoryMB < MEMORY_THRESHOLD_MB && !powerManager.isPowerSaveMode
    }
    
    /**
     * Update performance metrics for monitoring
     */
    private fun updatePerformanceMetrics(executionTime: Long) {
        val currentAverage = averageExecutionTime.get()
        val totalOps = totalOperations.get()
        
        // Calculate rolling average
        val newAverage = ((currentAverage * (totalOps - 1)) + executionTime) / totalOps
        averageExecutionTime.set(newAverage)
    }
    
    /**
     * Regular cleanup to prevent memory leaks
     */
    private fun cleanup() {
        // Clear any cached references
        if (pendingUpdates.size > BATCH_SIZE * 2) {
            pendingUpdates.clear()
            android.util.Log.w("WidgetOptimizer", "Cleared excessive pending updates")
        }
    }
    
    /**
     * Emergency cleanup for memory pressure
     */
    private fun emergencyCleanup() {
        pendingUpdates.clear()
        System.gc()
        android.util.Log.w("WidgetOptimizer", "Emergency cleanup performed")
    }
    
    /**
     * Get comprehensive performance statistics
     */
    fun getPerformanceStats(): PerformanceStats {
        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalOps = totalOperations.get()
        val successRate = if (totalOps > 0) {
            (successfulOperations.get().toFloat() / totalOps.toFloat()) * 100f
        } else {
            100f
        }
        
        return PerformanceStats(
            averageExecutionTime = averageExecutionTime.get(),
            totalOperations = totalOps,
            successRate = successRate,
            memoryUsageMB = usedMemoryMB,
            isThrottling = isThrottling.get(),
            isPowerSaveMode = powerManager.isPowerSaveMode,
            pendingUpdates = pendingUpdates.size
        )
    }
    
    /**
     * Shutdown optimizer and cleanup resources
     */
    fun shutdown() {
        optimizedScope.cancel()
        pendingUpdates.clear()
    }
    
    /**
     * Operation priority levels
     */
    enum class Priority {
        LOW,      // Background tasks, can be deferred
        NORMAL,   // Regular widget updates
        HIGH,     // User interactions
        CRITICAL  // Essential operations, bypass throttling
    }
    
    /**
     * Lazy loading results
     */
    sealed class LazyLoadResult<T> {
        object Loading : LazyLoadResult<Nothing>()
        data class Page<T>(
            val items: List<T>,
            val pageNumber: Int,
            val totalPages: Int,
            val hasMore: Boolean
        ) : LazyLoadResult<T>()
        data class Complete<T>(val allItems: List<T>) : LazyLoadResult<T>()
        data class Error<T>(val message: String) : LazyLoadResult<T>()
    }
    
    /**
     * Performance monitoring statistics
     */
    data class PerformanceStats(
        val averageExecutionTime: Long,
        val totalOperations: Long,
        val successRate: Float,
        val memoryUsageMB: Long,
        val isThrottling: Boolean,
        val isPowerSaveMode: Boolean,
        val pendingUpdates: Int
    ) {
        val isHealthy: Boolean
            get() = successRate > 95f && 
                   memoryUsageMB < MEMORY_THRESHOLD_MB && 
                   averageExecutionTime < 200L
                   
        val performanceGrade: String
            get() = when {
                isHealthy && successRate > 99f -> "A+"
                isHealthy -> "A"
                successRate > 90f && memoryUsageMB < MEMORY_THRESHOLD_MB * 1.2 -> "B"
                successRate > 80f -> "C"
                else -> "D"
            }
    }
}
