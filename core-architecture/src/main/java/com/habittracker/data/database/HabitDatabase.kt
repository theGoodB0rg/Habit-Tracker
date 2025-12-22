package com.habittracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.habittracker.data.database.converter.DatabaseConverters
import com.habittracker.data.database.dao.HabitDao
import com.habittracker.data.database.dao.HabitCompletionDao
import com.habittracker.data.database.dao.timing.*
import com.habittracker.data.database.entity.HabitEntity
import com.habittracker.data.database.entity.HabitCompletionEntity
import com.habittracker.data.database.entity.timing.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room database class for the Habit Tracker app.
 * Manages the SQLite database and provides access to DAOs.
 * Enhanced with smart timing capabilities for Phase 1.
 * 
 * This is a copy for core-architecture module to enable widget functionality.
 */
@Database(
    entities = [
        HabitEntity::class, 
        HabitCompletionEntity::class,
        // Phase 1 - Smart Timing Enhancement entities
        HabitTimingEntity::class,
        TimerSessionEntity::class,
        SmartSuggestionEntity::class,
        CompletionMetricsEntity::class,
        HabitAnalyticsEntity::class,
        // Phase UIX-1
        TimerAlertProfileEntity::class,
        PartialSessionEntity::class
    ],
        version = 8, // Incremented for period key on completions
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class HabitDatabase : RoomDatabase() {
    
    // Original DAOs
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    
    // Phase 1 - Smart Timing Enhancement DAOs
    abstract fun habitTimingDao(): HabitTimingDao
    abstract fun timerSessionDao(): TimerSessionDao
    abstract fun smartSuggestionDao(): SmartSuggestionDao
    abstract fun completionMetricsDao(): CompletionMetricsDao
    abstract fun habitAnalyticsDao(): HabitAnalyticsDao
    // Phase UIX-1
    abstract fun timerAlertProfileDao(): TimerAlertProfileDao
    // Partial sessions
    abstract fun partialSessionDao(): PartialSessionDao
    
    companion object {
        private const val DATABASE_NAME = "habit_database"
        
        @Volatile
        private var INSTANCE: HabitDatabase? = null
        
        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                // For very old installs (DB version 1 or 2), fall back to destructive migration.
                // We keep explicit migrations for 3+ to preserve user data where possible.
                .fallbackToDestructiveMigrationFrom(1, 2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultProfilesAsync(context)
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Ensure defaults exist (idempotent upsert)
                        seedDefaultProfilesAsync(context)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getInstance(context: Context): HabitDatabase = getDatabase(context)

        // Migration adding timer columns & new profile table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new nullable columns with safe defaults
                database.execSQL("ALTER TABLE habits ADD COLUMN timerEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE habits ADD COLUMN customDurationMinutes INTEGER")
                database.execSQL("ALTER TABLE habits ADD COLUMN alertProfileId TEXT")
                // New table for timer alert profiles
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `timer_alert_profiles` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `displayName` TEXT NOT NULL,
                            `description` TEXT,
                            `thresholdsJson` TEXT NOT NULL,
                            `isUserEditable` INTEGER NOT NULL,
                            `createdAtEpochMillis` INTEGER NOT NULL
                        )
                    """.trimIndent()
                )
            }
        }

        // Migration adding guard columns to habit_timing
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Safe nullable additions
                database.execSQL("ALTER TABLE habit_timing ADD COLUMN min_duration_minutes INTEGER")
                database.execSQL("ALTER TABLE habit_timing ADD COLUMN require_timer_to_complete INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration adding auto_complete_on_target flag to habit_timing
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habit_timing ADD COLUMN auto_complete_on_target INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration adding partial_sessions table
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `partial_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habit_id` INTEGER NOT NULL,
                        `duration_minutes` INTEGER NOT NULL,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_partial_sessions_habit_id` ON `partial_sessions` (`habit_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_partial_sessions_created_at` ON `partial_sessions` (`created_at`)")
            }
        }

        // Migration adding periodKey to habit_completions with per-period uniqueness
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habit_completions ADD COLUMN periodKey TEXT NOT NULL DEFAULT ''")

                val freqByHabit = mutableMapOf<Long, String>()
                database.query("SELECT id, frequency FROM habits").use { cursor ->
                    val idIdx = cursor.getColumnIndex("id")
                    val freqIdx = cursor.getColumnIndex("frequency")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val freq = cursor.getString(freqIdx) ?: "DAILY"
                        freqByHabit[id] = freq
                    }
                }

                database.query("SELECT id, habitId, completedDate FROM habit_completions").use { cursor ->
                    val idIdx = cursor.getColumnIndex("id")
                    val habitIdx = cursor.getColumnIndex("habitId")
                    val dateIdx = cursor.getColumnIndex("completedDate")
                    while (cursor.moveToNext()) {
                        val rowId = cursor.getLong(idIdx)
                        val habitId = cursor.getLong(habitIdx)
                        val dateStr = cursor.getString(dateIdx) ?: continue
                        val freq = freqByHabit[habitId] ?: "DAILY"
                        val periodKey = computePeriodKey(freq, dateStr)
                        database.execSQL(
                            "UPDATE habit_completions SET periodKey = ? WHERE id = ?",
                            arrayOf(periodKey, rowId)
                        )
                    }
                }

                database.execSQL("UPDATE habit_completions SET periodKey = completedDate WHERE periodKey = '' OR periodKey IS NULL")

                // Deduplicate by period key per habit, keep smallest id
                val toDelete = mutableListOf<Pair<Long, String>>()
                database.query(
                    """
                        SELECT habitId, periodKey, MIN(id) as keepId
                        FROM habit_completions
                        GROUP BY habitId, periodKey
                        HAVING COUNT(*) > 1
                    """
                ).use { cursor ->
                    val habitIdx = cursor.getColumnIndex("habitId")
                    val keyIdx = cursor.getColumnIndex("periodKey")
                    val keepIdx = cursor.getColumnIndex("keepId")
                    while (cursor.moveToNext()) {
                        val habitId = cursor.getLong(habitIdx)
                        val key = cursor.getString(keyIdx)
                        val keepId = cursor.getLong(keepIdx)
                        database.execSQL(
                            "DELETE FROM habit_completions WHERE habitId = ? AND periodKey = ? AND id != ?",
                            arrayOf(habitId, key, keepId)
                        )
                    }
                }

                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habitId_periodKey` ON `habit_completions` (`habitId`, `periodKey`)")
            }

            private fun computePeriodKey(freq: String, dateStr: String): String {
                return try {
                    val date = java.time.LocalDate.parse(dateStr)
                    when (freq) {
                        "WEEKLY" -> {
                            val week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                            val year = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                            "%04d-W%02d".format(year, week)
                        }
                        "MONTHLY" -> "%04d-%02d".format(date.year, date.monthValue)
                        else -> date.toString()
                    }
                } catch (_: Exception) {
                    dateStr
                }
            }
        }

        private fun seedDefaultProfilesAsync(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = getDatabase(context)
                    val dao = db.timerAlertProfileDao()
                    // Predefined profiles (idempotent upsert)
                    val quiet = TimerAlertProfileEntity(
                        id = "quiet",
                        displayName = "Quiet",
                        description = "Minimal alerts (start, midpoint, end)",
                        thresholdsJson = "[0,50,100]",
                        isUserEditable = false
                    )
                    val focus = TimerAlertProfileEntity(
                        id = "focus",
                        displayName = "Focus",
                        description = "Key progress alerts (every 25%)",
                        thresholdsJson = "[0,25,50,75,100]",
                        isUserEditable = false
                    )
                    val verbose = TimerAlertProfileEntity(
                        id = "verbose",
                        displayName = "Verbose",
                        description = "Frequent alerts (every 10%)",
                        thresholdsJson = "[0,10,20,30,40,50,60,70,80,90,100]",
                        isUserEditable = false
                    )
                    dao.upsertProfiles(quiet, focus, verbose)
                } catch (_: Exception) {
                    // Swallow seeding errors (non-fatal)
                }
            }
        }
    }
}
