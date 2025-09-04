package com.habittracker.analytics.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_visit")
data class ScreenVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val screenName: String,
    val visitTimestamp: Long
) {
    init {
        require(screenName.isNotBlank()) { "Screen name cannot be blank" }
        require(visitTimestamp > 0) { "Visit timestamp must be a positive value" }
    }
}
