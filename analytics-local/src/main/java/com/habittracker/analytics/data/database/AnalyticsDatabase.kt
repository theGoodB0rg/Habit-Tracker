package com.habittracker.analytics.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.habittracker.analytics.data.database.entities.*

/**
 * Enhanced Analytics Database with proper versioning, migrations, and performance optimizations
 */
@Database(
    entities = [
        HabitCompletionEntity::class,
        ScreenVisitEntity::class,
        StreakRetentionEntity::class,
        HabitCompletionAnalyticsEntity::class,
        ScreenVisitAnalyticsEntity::class,
        StreakRetentionAnalyticsEntity::class,
        UserEngagementEntity::class,
        AppSessionEntity::class,
        PerformanceMetricEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AnalyticsTypeConverters::class)
abstract class AnalyticsDatabase : RoomDatabase() {
    
    abstract fun analyticsDao(): AnalyticsDao
    
    companion object {
        private const val DATABASE_NAME = "analytics_database"
        
        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null
        
        fun getDatabase(context: Context): AnalyticsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .enableMultiInstanceInvalidation()
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // Better performance for concurrent access
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Migration from version 1 to 2
         * Adds new comprehensive analytics tables
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create user engagement analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_engagement_analytics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        sessionCount INTEGER NOT NULL,
                        totalTimeSpentMs INTEGER NOT NULL,
                        averageSessionMs INTEGER NOT NULL,
                        screenTransitions INTEGER NOT NULL,
                        habitsInteracted INTEGER NOT NULL,
                        habitsCompleted INTEGER NOT NULL,
                        deepEngagement INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // Create app session analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_session_analytics (
                        sessionId TEXT PRIMARY KEY NOT NULL,
                        startTimestamp INTEGER NOT NULL,
                        endTimestamp INTEGER,
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        durationMs INTEGER NOT NULL,
                        screenVisits INTEGER NOT NULL,
                        habitInteractions INTEGER NOT NULL,
                        backgroundedCount INTEGER NOT NULL,
                        crashed INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // Create performance metrics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS performance_metrics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        metricType TEXT NOT NULL,
                        value REAL NOT NULL,
                        unit TEXT NOT NULL,
                        context TEXT,
                        deviceInfo TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Create indexes for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_engagement_analytics_date ON user_engagement_analytics(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_app_session_analytics_sessionId ON app_session_analytics(sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_performance_metrics_date ON performance_metrics(date)")
            }
        }

        /**
         * Migration from version 2 to 3
         * Adds generic analytics_events table for lightweight event tracking
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS analytics_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        additionalData TEXT
                    )
                    """
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_analytics_events_eventType ON analytics_events(eventType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_analytics_events_timestamp ON analytics_events(timestamp)")
            }
        }
        
        /**
         * Clear all analytics data (for testing or user privacy requests)
         */
        suspend fun clearAllData(context: Context) {
            val database = getDatabase(context)
            database.clearAllTables()
        }
    }
}