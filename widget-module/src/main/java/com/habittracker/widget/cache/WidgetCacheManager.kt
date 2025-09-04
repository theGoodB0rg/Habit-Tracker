package com.habittracker.widget.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.habittracker.core.HabitWidgetData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Professional caching system for widget data with advanced performance optimization.
 * 
 * Features:
 * - LRU memory cache for ultra-fast access
 * - Encrypted persistent disk cache for offline reliability
 * - Intelligent cache invalidation strategies
 * - Background sync and preloading
 * - Performance monitoring and analytics
 * 
 * Performance Targets:
 * - Memory cache hit time: <1ms
 * - Disk cache hit time: <10ms
 * - Cache hit rate: >99%
 * - Memory usage: <10MB
 */
class WidgetCacheManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WidgetCacheManager? = null
        
        fun getInstance(context: Context): WidgetCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Cache configuration
        private const val MEMORY_CACHE_SIZE = 100 // Maximum items in memory
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
        private const val PREFS_NAME = "widget_cache_prefs"
        private const val KEY_HABITS_CACHE = "habits_cache"
        private const val KEY_PROGRESS_CACHE = "progress_cache"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
    }
    
    // Thread-safe memory cache implementation
    private val memoryCacheMutex = Mutex()
    private val habitsMemoryCache = ConcurrentHashMap<String, CacheEntry<List<HabitWidgetData>>>()
    private val progressMemoryCache = ConcurrentHashMap<String, CacheEntry<ProgressStats>>()
    
    // Encrypted persistent storage
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }
    
    // Performance monitoring
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Cached data container with expiration tracking
     */
    @Serializable
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val accessCount: Long = 0
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS
            
        fun accessed(): CacheEntry<T> = copy(accessCount = accessCount + 1)
    }
    
    /**
     * Serializable version of HabitWidgetData for disk caching
     */
    @Serializable
    private data class SerializableHabitData(
        val id: Long,
        val name: String,
        val description: String,
        val icon: Int,
        val isCompleted: Boolean = false,
        val currentStreak: Int = 0,
        val priority: Int = 0,
        val frequency: String = "DAILY",
        val lastCompletedDateEpoch: Long? = null
    ) {
        companion object {
            fun fromHabitWidgetData(habit: HabitWidgetData): SerializableHabitData {
                return SerializableHabitData(
                    id = habit.id,
                    name = habit.name,
                    description = habit.description,
                    icon = habit.icon,
                    isCompleted = habit.isCompleted,
                    currentStreak = habit.currentStreak,
                    priority = habit.priority,
                    frequency = habit.frequency,
                    lastCompletedDateEpoch = habit.lastCompletedDate?.toEpochDay()
                )
            }
        }
        
        fun toHabitWidgetData(): HabitWidgetData {
            return HabitWidgetData(
                id = id,
                name = name,
                description = description,
                icon = icon,
                isCompleted = isCompleted,
                currentStreak = currentStreak,
                priority = priority,
                frequency = frequency,
                lastCompletedDate = lastCompletedDateEpoch?.let { java.time.LocalDate.ofEpochDay(it) }
            )
        }
    }

    /**
     * Serializable progress stats for caching
     */
    @Serializable
    private data class SerializableProgressStats(
        val completedCount: Int,
        val totalCount: Int,
        val completionPercentage: Float
    )
    
    /**
     * Get cached habits data with intelligent fallback strategy
     */
    suspend fun getCachedHabits(): List<HabitWidgetData>? = memoryCacheMutex.withLock {
        val cacheKey = "today_habits"
        val currentDate = java.time.LocalDate.now()
        
        // Try memory cache first (fastest)
        habitsMemoryCache[cacheKey]?.let { entry ->
            // Check if cache is for today and not expired
            val cacheDate = java.time.Instant.ofEpochMilli(entry.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            
            if (!entry.isExpired && cacheDate.isEqual(currentDate)) {
                habitsMemoryCache[cacheKey] = entry.accessed()
                cacheHits.incrementAndGet()
                return@withLock entry.data
            } else {
                // Remove expired or old date entry
                habitsMemoryCache.remove(cacheKey)
            }
        }
        
        // Try disk cache (persistent)
        try {
            val cachedJson = encryptedPrefs.getString(KEY_HABITS_CACHE, null)
            val cacheTimestamp = encryptedPrefs.getLong(KEY_CACHE_TIMESTAMP, 0)
            val cacheDate = java.time.Instant.ofEpochMilli(cacheTimestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            
            if (cachedJson != null && 
                System.currentTimeMillis() - cacheTimestamp < CACHE_EXPIRY_MS &&
                cacheDate.isEqual(currentDate)) {
                val serializableHabits = json.decodeFromString<List<SerializableHabitData>>(cachedJson)
                val habits = serializableHabits.map { it.toHabitWidgetData() }
                
                // Restore to memory cache
                habitsMemoryCache[cacheKey] = CacheEntry(habits)
                cacheHits.incrementAndGet()
                return@withLock habits
            } else {
                // Clear old cache data if it's from a different date
                if (cacheDate.isBefore(currentDate)) {
                    clearHabitCache()
                }
            }
        } catch (e: Exception) {
            // Log cache restoration failure but don't crash
            android.util.Log.w("WidgetCache", "Failed to restore habits from disk cache: ${e.message}")
        }
        
        cacheMisses.incrementAndGet()
        null
    }
    
    /**
     * Cache habits data with intelligent storage strategy
     */
    suspend fun cacheHabits(habits: List<HabitWidgetData>) = memoryCacheMutex.withLock {
        val cacheKey = "today_habits"
        val entry = CacheEntry(habits)
        
        // Store in memory cache
        habitsMemoryCache[cacheKey] = entry
        
        // Maintain LRU cache size
        if (habitsMemoryCache.size > MEMORY_CACHE_SIZE) {
            evictLeastRecentlyUsed()
        }
        
        // Store in disk cache for persistence
        try {
            val serializableHabits = habits.map { SerializableHabitData.fromHabitWidgetData(it) }
            val habitsJson = json.encodeToString(serializableHabits)
            encryptedPrefs.edit()
                .putString(KEY_HABITS_CACHE, habitsJson)
                .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            android.util.Log.w("WidgetCache", "Failed to save habits to disk cache: ${e.message}")
        }
    }
    
    /**
     * Get cached progress stats with fast memory lookup
     */
    suspend fun getCachedProgress(): ProgressStats? = memoryCacheMutex.withLock {
        val cacheKey = "today_progress"
        
        // Try memory cache first
        progressMemoryCache[cacheKey]?.let { entry ->
            if (!entry.isExpired) {
                progressMemoryCache[cacheKey] = entry.accessed()
                cacheHits.incrementAndGet()
                return@withLock entry.data
            } else {
                progressMemoryCache.remove(cacheKey)
            }
        }
        
        // Try disk cache
        try {
            val cachedJson = encryptedPrefs.getString(KEY_PROGRESS_CACHE, null)
            val cacheTimestamp = encryptedPrefs.getLong(KEY_CACHE_TIMESTAMP, 0)
            
            if (cachedJson != null && System.currentTimeMillis() - cacheTimestamp < CACHE_EXPIRY_MS) {
                val progressData = json.decodeFromString<SerializableProgressStats>(cachedJson)
                val progress = ProgressStats(
                    completedCount = progressData.completedCount,
                    totalCount = progressData.totalCount,
                    completionPercentage = progressData.completionPercentage
                )
                
                // Restore to memory cache
                progressMemoryCache[cacheKey] = CacheEntry(progress)
                cacheHits.incrementAndGet()
                return@withLock progress
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetCache", "Failed to restore progress from disk cache: ${e.message}")
        }
        
        cacheMisses.incrementAndGet()
        null
    }
    
    /**
     * Cache progress stats with optimized serialization
     */
    suspend fun cacheProgress(progress: ProgressStats) = memoryCacheMutex.withLock {
        val cacheKey = "today_progress"
        val entry = CacheEntry(progress)
        
        // Store in memory cache
        progressMemoryCache[cacheKey] = entry
        
        // Store in disk cache
        try {
            val progressData = SerializableProgressStats(
                completedCount = progress.completedCount,
                totalCount = progress.totalCount,
                completionPercentage = progress.completionPercentage
            )
            val progressJson = json.encodeToString(progressData)
            encryptedPrefs.edit()
                .putString(KEY_PROGRESS_CACHE, progressJson)
                .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            android.util.Log.w("WidgetCache", "Failed to save progress to disk cache: ${e.message}")
        }
    }
    
    /**
     * Invalidate all cached data (force refresh)
     */
    suspend fun invalidateCache() = memoryCacheMutex.withLock {
        habitsMemoryCache.clear()
        progressMemoryCache.clear()
        
        encryptedPrefs.edit()
            .remove(KEY_HABITS_CACHE)
            .remove(KEY_PROGRESS_CACHE)
            .remove(KEY_CACHE_TIMESTAMP)
            .apply()
    }
    
    /**
     * Smart cache invalidation for specific habit changes
     */
    suspend fun invalidateHabit(habitId: Long) = memoryCacheMutex.withLock {
        // For now, invalidate all cache since we don't have per-habit caching
        // In future enhancement, could implement granular invalidation
        invalidateCache()
    }
    
    /**
     * Clear habit-specific cache data when date changes
     */
    private suspend fun clearHabitCache() = memoryCacheMutex.withLock {
        habitsMemoryCache.clear()
        
        // Clear habit-related preferences
        encryptedPrefs.edit()
            .remove(KEY_HABITS_CACHE)
            .remove(KEY_CACHE_TIMESTAMP)
            .apply()
    }
    
    /**
     * Clear all cached data and reset cache state
     */
    suspend fun clearAll() = memoryCacheMutex.withLock {
        habitsMemoryCache.clear()
        progressMemoryCache.clear()
        
        // Clear all widget-related preferences
        encryptedPrefs.edit()
            .clear()
            .apply()
            
        // Reset cache statistics
        cacheHits.set(0)
        cacheMisses.set(0)
    }
    
    /**
     * LRU eviction policy implementation
     */
    private fun evictLeastRecentlyUsed() {
        // Find and remove the least recently accessed entry
        val lruHabitsEntry = habitsMemoryCache.entries
            .minByOrNull { it.value.accessCount }
        lruHabitsEntry?.let { habitsMemoryCache.remove(it.key) }
        
        val lruProgressEntry = progressMemoryCache.entries
            .minByOrNull { it.value.accessCount }
        lruProgressEntry?.let { progressMemoryCache.remove(it.key) }
    }
    
    /**
     * Get cache performance statistics
     */
    fun getCacheStats(): CacheStats {
        val totalRequests = cacheHits.get() + cacheMisses.get()
        val hitRate = if (totalRequests > 0) {
            (cacheHits.get().toFloat() / totalRequests.toFloat()) * 100f
        } else {
            0f
        }
        
        return CacheStats(
            hitRate = hitRate,
            totalHits = cacheHits.get(),
            totalMisses = cacheMisses.get(),
            memoryEntries = habitsMemoryCache.size + progressMemoryCache.size,
            lastUpdateTime = encryptedPrefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        )
    }
    
    /**
     * Warm up cache with background preloading
     */
    suspend fun warmupCache(habits: List<HabitWidgetData>, progress: ProgressStats) {
        cacheHabits(habits)
        cacheProgress(progress)
    }
    
    /**
     * Cache performance statistics
     */
    data class CacheStats(
        val hitRate: Float,
        val totalHits: Long,
        val totalMisses: Long,
        val memoryEntries: Int,
        val lastUpdateTime: Long
    ) {
        val isHealthy: Boolean
            get() = hitRate > 95f && memoryEntries < MEMORY_CACHE_SIZE
            
        val isExpired: Boolean
            get() = System.currentTimeMillis() - lastUpdateTime > CACHE_EXPIRY_MS
    }
}

/**
 * Progress statistics data class for caching
 */
data class ProgressStats(
    val completedCount: Int,
    val totalCount: Int,
    val completionPercentage: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getProgressText(): String = "$completedCount/$totalCount (${(completionPercentage * 100).toInt()}%)"
    
    fun getDailySummaryText(): String = when {
        totalCount == 0 -> "No habits for today"
        completionPercentage >= 1.0f -> "🎉 All habits complete!"
        completionPercentage > 0.0f -> "$completedCount of $totalCount complete"
        else -> "Ready to start your day"
    }
    
    fun isAllCompleted(): Boolean = completionPercentage >= 1.0f && totalCount > 0
    fun isNoneCompleted(): Boolean = completionPercentage == 0.0f
}
