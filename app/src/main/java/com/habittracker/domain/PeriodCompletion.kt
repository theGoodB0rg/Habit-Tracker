package com.habittracker.domain

import com.habittracker.data.database.entity.HabitFrequency
import java.time.LocalDate
import java.time.temporal.WeekFields

/**
 * Returns true when the last completion date falls within the current recurrence period
 * defined by the habit frequency (daily/weekly/monthly).
 */
fun isCompletedThisPeriod(
    frequency: HabitFrequency,
    lastCompletedDate: LocalDate?,
    today: LocalDate = LocalDate.now()
): Boolean {
    val last = lastCompletedDate ?: return false
    return when (frequency) {
        HabitFrequency.DAILY -> last == today
        HabitFrequency.WEEKLY -> {
            val wf = WeekFields.ISO
            val lastWeek = last.get(wf.weekOfWeekBasedYear())
            val todayWeek = today.get(wf.weekOfWeekBasedYear())
            val lastYear = last.get(wf.weekBasedYear())
            val todayYear = today.get(wf.weekBasedYear())
            lastWeek == todayWeek && lastYear == todayYear
        }
        HabitFrequency.MONTHLY -> last.year == today.year && last.month == today.month
    }
}
