package com.habittracker.analytics.utils

import com.habittracker.analytics.domain.models.TimeFrame
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced date utilities for comprehensive analytics with proper time zone handling
 */
@Singleton
class DateUtils @Inject constructor() {

    companion object {
        private const val ISO_DATE_FORMAT = "yyyy-MM-dd"
        private const val ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
        
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT)
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATETIME_FORMAT)
    }

    /**
     * Get current date as ISO string
     */
    fun getCurrentDateString(): String {
        return LocalDate.now().format(DATE_FORMATTER)
    }

    /**
     * Convert LocalDate to ISO string
     */
    fun getDateString(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    /**
     * Parse ISO date string to LocalDate
     */
    fun parseDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString, DATE_FORMATTER)
    }

    /**
     * Get date range for different time frames
     */
    fun getDateRange(timeFrame: TimeFrame): Pair<String, String> {
        val endDate = LocalDate.now()
        val startDate = when (timeFrame) {
            TimeFrame.DAILY -> endDate
            TimeFrame.WEEKLY -> endDate.minusWeeks(1)
            TimeFrame.MONTHLY -> endDate.minusMonths(1)
            TimeFrame.QUARTERLY -> endDate.minusMonths(3)
            TimeFrame.YEARLY -> endDate.minusYears(1)
            TimeFrame.ALL_TIME -> LocalDate.of(2020, 1, 1) // Reasonable start date
        }
        
        return Pair(getDateString(startDate), getDateString(endDate))
    }

    /**
     * Get number of days between two dates
     */
    fun getDaysBetween(startDate: LocalDate, endDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(startDate, endDate)
    }

    /**
     * Get number of weeks between two dates
     */
    fun getWeeksBetween(startDate: LocalDate, endDate: LocalDate): Long {
        return ChronoUnit.WEEKS.between(startDate, endDate)
    }

    /**
     * Get number of months between two dates
     */
    fun getMonthsBetween(startDate: LocalDate, endDate: LocalDate): Long {
        return ChronoUnit.MONTHS.between(startDate, endDate)
    }

    /**
     * Check if two dates are in the same week
     */
    fun isSameWeek(date1: LocalDate, date2: LocalDate): Boolean {
        return getWeeksBetween(date1, date2) == 0L
    }

    /**
     * Check if two dates are in the same month
     */
    fun isSameMonth(date1: LocalDate, date2: LocalDate): Boolean {
        return date1.year == date2.year && date1.month == date2.month
    }

    /**
     * Get start of week for a given date
     */
    fun getStartOfWeek(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value - 1L)
    }

    /**
     * Get end of week for a given date
     */
    fun getEndOfWeek(date: LocalDate): LocalDate {
        return date.plusDays(7L - date.dayOfWeek.value)
    }

    /**
     * Get start of month for a given date
     */
    fun getStartOfMonth(date: LocalDate): LocalDate {
        return date.withDayOfMonth(1)
    }

    /**
     * Get end of month for a given date
     */
    fun getEndOfMonth(date: LocalDate): LocalDate {
        return date.withDayOfMonth(date.lengthOfMonth())
    }

    /**
     * Get list of dates between start and end dates
     */
    fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }
        
        return dates
    }

    /**
     * Get list of weeks between start and end dates
     */
    fun getWeeksBetweenDates(startDate: LocalDate, endDate: LocalDate): List<Pair<LocalDate, LocalDate>> {
        val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
        var currentWeekStart = getStartOfWeek(startDate)
        
        while (!currentWeekStart.isAfter(endDate)) {
            val weekEnd = getEndOfWeek(currentWeekStart)
            weeks.add(Pair(currentWeekStart, weekEnd))
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }
        
        return weeks
    }

    /**
     * Check if date is today
     */
    fun isToday(date: LocalDate): Boolean {
        return date.isEqual(LocalDate.now())
    }

    /**
     * Check if date is yesterday
     */
    fun isYesterday(date: LocalDate): Boolean {
        return date.isEqual(LocalDate.now().minusDays(1))
    }

    /**
     * Check if date is within last N days
     */
    fun isWithinLastNDays(date: LocalDate, days: Int): Boolean {
        val cutoffDate = LocalDate.now().minusDays(days.toLong())
        return date.isAfter(cutoffDate) || date.isEqual(cutoffDate)
    }

    /**
     * Get human readable time difference
     */
    fun getTimeAgo(date: LocalDate): String {
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(date, today)
        
        return when {
            daysDiff == 0L -> "Today"
            daysDiff == 1L -> "Yesterday"
            daysDiff < 7L -> "$daysDiff days ago"
            daysDiff < 30L -> "${daysDiff / 7} weeks ago"
            daysDiff < 365L -> "${daysDiff / 30} months ago"
            else -> "${daysDiff / 365} years ago"
        }
    }

    /**
     * Get current timestamp in milliseconds
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Convert timestamp to LocalDate
     */
    fun timestampToDate(timestamp: Long): LocalDate {
        return LocalDate.ofEpochDay(timestamp / (24 * 60 * 60 * 1000))
    }

    /**
     * Convert LocalDate to timestamp (start of day)
     */
    fun dateToTimestamp(date: LocalDate): Long {
        return date.toEpochDay() * 24 * 60 * 60 * 1000
    }
}