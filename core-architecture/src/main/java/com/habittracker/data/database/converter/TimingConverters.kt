package com.habittracker.data.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Type converters for timing-related data types
 * Part of Phase 1 - Smart Timing Enhancement
 *
 * Note: We avoid generic List converters to prevent Room conflicts
 */
class TimingConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromLocalTime(localTime: LocalTime?): String? {
        return localTime?.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }
    }

    @TypeConverter
    fun fromLocalDateTime(localDateTime: LocalDateTime?): Long? {
        return localDateTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDateTime(timestamp: Long?): LocalDateTime? {
        return timestamp?.let {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it),
                java.time.ZoneOffset.UTC
            )
        }
    }

    @TypeConverter
    fun fromDuration(duration: Duration?): Int? {
        return duration?.toMinutes()?.toInt()
    }

    @TypeConverter
    fun toDuration(minutes: Int?): Duration? {
        return minutes?.let { Duration.ofMinutes(it.toLong()) }
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toStringMap(mapJson: String?): Map<String, String>? {
        if (mapJson == null) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(mapJson, type)
    }

    @TypeConverter
    fun fromIntFloatMap(map: Map<Int, Float>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toIntFloatMap(mapJson: String?): Map<Int, Float>? {
        if (mapJson == null) return null
        val type = object : TypeToken<Map<Int, Float>>() {}.type
        return gson.fromJson(mapJson, type)
    }
}
