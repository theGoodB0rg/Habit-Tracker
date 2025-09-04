package com.habittracker.data.database.converter

import androidx.room.TypeConverter
import com.habittracker.data.database.entity.HabitFrequency
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

/**
 * Room type converters for custom data types
 * UNIFIED VERSION - Compatible with all modules
 */
class DatabaseConverters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }
    
    @TypeConverter
    fun localDateTimeToString(dateTime: LocalDateTime?): String? {
        return dateTime?.toString()
    }
    
    @TypeConverter
    fun fromHabitFrequency(frequency: HabitFrequency): String {
        return frequency.name
    }
    
    @TypeConverter
    fun toHabitFrequency(frequency: String): HabitFrequency {
        return HabitFrequency.valueOf(frequency)
    }
}
