package com.habittracker.analytics.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val timestamp: Date,
    val additionalData: String? = null
) {
    // Additional logic can be added here if needed
}