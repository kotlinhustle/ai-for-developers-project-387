package com.example.backend.slots

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SlotCalculator(
    private val workingHours: WorkingHours,
    private val zone: ZoneId,
    private val step: Duration = Duration.ofMinutes(30),
    private val windowDays: Long = 14,
) {
    fun freeSlots(
        now: Instant,
        durationMinutes: Int,
        booked: List<TimeRange>,
        from: LocalDate?,
        to: LocalDate?,
    ): List<Instant> {
        val today = now.atZone(zone).toLocalDate()
        val windowEnd = today.plusDays(windowDays - 1)
        val firstDate = maxOf(from ?: today, today)
        val lastDate = minOf(to ?: windowEnd, windowEnd)

        val duration = Duration.ofMinutes(durationMinutes.toLong())
        val slots = mutableListOf<Instant>()

        var date = firstDate
        while (!date.isAfter(lastDate)) {
            if (workingHours.days.contains(date.dayOfWeek)) {
                val dayEnd = date.atTime(workingHours.end).atZone(zone).toInstant()
                var start = date.atTime(workingHours.start).atZone(zone).toInstant()

                while (!start.plus(duration).isAfter(dayEnd)) {
                    val candidate = TimeRange(start, start.plus(duration))
                    if (start.isAfter(now) && booked.none { it.overlaps(candidate) }) {
                        slots.add(start)
                    }
                    start = start.plus(step)
                }
            }
            date = date.plusDays(1)
        }

        return slots
    }
}
