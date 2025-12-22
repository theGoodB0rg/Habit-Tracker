package com.habittracker.core

import com.habittracker.data.database.entity.HabitFrequency
import java.time.LocalDate
import java.time.temporal.IsoFields

object PeriodKeyCalculator {
    fun fromDate(frequency: HabitFrequency, date: LocalDate): String {
        return when (frequency) {
            HabitFrequency.DAILY -> date.toString()
            HabitFrequency.WEEKLY -> {
                val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val year = date.get(IsoFields.WEEK_BASED_YEAR)
                "%04d-W%02d".format(year, week)
            }
            HabitFrequency.MONTHLY -> {
                "%04d-%02d".format(date.year, date.monthValue)
            }
        }
    }
}
