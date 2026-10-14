package com.example.backend.booking

import com.example.backend.slots.SlotCalculator
import com.example.backend.slots.TimeRange
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

sealed interface BookingValidation {
    data object Valid : BookingValidation

    data class Invalid(
        val reason: String,
    ) : BookingValidation

    data object Conflict : BookingValidation
}

class BookingPolicy(
    private val slotCalculator: SlotCalculator,
    private val zone: ZoneId,
) {
    fun validate(
        now: Instant,
        durationMinutes: Int,
        start: Instant,
        booked: List<TimeRange>,
    ): BookingValidation {
        val day = start.atZone(zone).toLocalDate()
        val bookable = slotCalculator.freeSlots(now, durationMinutes, emptyList(), day, day)
        if (start !in bookable) {
            return BookingValidation.Invalid("slot is not bookable")
        }

        val end = start.plus(Duration.ofMinutes(durationMinutes.toLong()))
        if (booked.any { it.overlaps(TimeRange(start, end)) }) {
            return BookingValidation.Conflict
        }

        return BookingValidation.Valid
    }
}
