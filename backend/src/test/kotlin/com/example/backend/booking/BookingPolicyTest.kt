package com.example.backend.booking

import com.example.backend.slots.SlotCalculator
import com.example.backend.slots.TimeRange
import com.example.backend.slots.WorkingHours
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class BookingPolicyTest {
    private val zone: ZoneId = ZoneId.of("Asia/Yerevan")
    private val policy = BookingPolicy(SlotCalculator(WorkingHours.weekdays9to18, zone), zone)
    private val monday: LocalDate = LocalDate.of(2026, 10, 12)
    private val now = monday.atTime(0, 0).atZone(zone).toInstant()

    private fun at(
        hour: Int,
        minute: Int = 0,
    ) = monday.atTime(hour, minute).atZone(zone).toInstant()

    @Test
    fun `accepts a free slot on the grid`() {
        assertEquals(BookingValidation.Valid, policy.validate(now, 30, at(10), emptyList()))
    }

    @Test
    fun `rejects a slot that is not on the grid`() {
        assertTrue(policy.validate(now, 30, at(10, 15), emptyList()) is BookingValidation.Invalid)
    }

    @Test
    fun `rejects a slot outside working hours`() {
        assertTrue(policy.validate(now, 30, at(18), emptyList()) is BookingValidation.Invalid)
    }

    @Test
    fun `rejects a slot in the past`() {
        val later = monday.atTime(12, 0).atZone(zone).toInstant()

        assertTrue(policy.validate(later, 30, at(10), emptyList()) is BookingValidation.Invalid)
    }

    @Test
    fun `reports a conflict when the slot overlaps an existing booking`() {
        val booked = listOf(TimeRange(at(10), at(10).plusSeconds(1800)))

        assertEquals(BookingValidation.Conflict, policy.validate(now, 30, at(10), booked))
    }

    @Test
    fun `rejects a slot beyond the 14-day window`() {
        val beyondWindow =
            monday
                .plusDays(14)
                .atTime(10, 0)
                .atZone(zone)
                .toInstant()

        assertTrue(policy.validate(now, 30, beyondWindow, emptyList()) is BookingValidation.Invalid)
    }
}
