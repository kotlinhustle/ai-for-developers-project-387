package com.example.backend.slots

import java.time.DayOfWeek
import java.time.LocalTime

data class WorkingHours(
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
) {
    companion object {
        val weekdays9to18 =
            WorkingHours(
                days =
                    setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                    ),
                start = LocalTime.of(9, 0),
                end = LocalTime.of(18, 0),
            )
    }
}
