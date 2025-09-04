package com.habittracker.data.database.entity.timing

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase UIX-1: Defines reusable alert profiles for timer sessions.
 * thresholdsJson is a serialized representation (e.g., JSON array) of alert thresholds in minutes/seconds.
 */
@Entity(tableName = "timer_alert_profiles")
data class TimerAlertProfileEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String, // e.g. "quiet", "focus", "verbose" or custom uuid
    val displayName: String,
    val description: String? = null,
    val thresholdsJson: String, // serialized list of alert threshold specs
    val isUserEditable: Boolean = true,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
