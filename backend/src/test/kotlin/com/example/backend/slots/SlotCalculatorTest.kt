package com.example.backend.slots

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SlotCalculatorTest {
    private val zone: ZoneId = ZoneId.of("Asia/Yerevan")
    private val calculator = SlotCalculator(WorkingHours.weekdays9to18, zone)
    private val monday: LocalDate = LocalDate.of(2026, 10, 12)

    private fun startOfDay(date: LocalDate) = date.atTime(0, 0).atZone(zone).toInstant()

    @Test
    fun `generates a 30-minute grid that fits inside working hours`() {
        val slots = calculator.freeSlots(startOfDay(monday), 30, emptyList(), monday, monday)

        assertEquals(18, slots.size)
        assertEquals(LocalTime.of(9, 0), slots.first().atZone(zone).toLocalTime())
        assertEquals(LocalTime.of(17, 30), slots.last().atZone(zone).toLocalTime())
    }

    @Test
    fun `excludes slots that overlap an existing booking`() {
        val bookingStart = monday.atTime(9, 0).atZone(zone).toInstant()
        val booked = listOf(TimeRange(bookingStart, bookingStart.plusSeconds(1800)))

        val slots = calculator.freeSlots(startOfDay(monday), 30, booked, monday, monday)

        assertFalse(slots.contains(bookingStart))
        assertEquals(17, slots.size)
    }

    @Test
    fun `skips weekend days`() {
        val saturday = LocalDate.of(2026, 10, 17)

        val slots = calculator.freeSlots(startOfDay(saturday), 30, emptyList(), saturday, saturday)

        assertTrue(slots.isEmpty())
    }

    @Test
    fun `returns only future slots`() {
        val now = monday.atTime(12, 0).atZone(zone).toInstant()

        val slots = calculator.freeSlots(now, 30, emptyList(), monday, monday)

        assertEquals(11, slots.size)
        assertTrue(slots.all { it.isAfter(now) })
    }

    @Test
    fun `limits the window to 14 days from today`() {
        val slots = calculator.freeSlots(startOfDay(monday), 30, emptyList(), null, null)

        val lastDay = slots.last().atZone(zone).toLocalDate()
        assertTrue(lastDay.isBefore(monday.plusDays(14)))
    }

    @Test
    fun `respects the requested from and to bounds`() {
        val tuesday = monday.plusDays(1)

        val slots = calculator.freeSlots(startOfDay(monday), 30, emptyList(), tuesday, tuesday)

        assertTrue(slots.isNotEmpty())
        assertTrue(slots.all { it.atZone(zone).toLocalDate() == tuesday })
    }

    @Test
    fun `excludes a meeting that would overflow working hours`() {
        val slots = calculator.freeSlots(startOfDay(monday), 60, emptyList(), monday, monday)

        assertEquals(LocalTime.of(17, 0), slots.last().atZone(zone).toLocalTime())
    }

    @Test
    fun `does not exclude a slot that starts exactly when a booking ends`() {
        val bookingStart = monday.atTime(9, 0).atZone(zone).toInstant()
        val booked = listOf(TimeRange(bookingStart, bookingStart.plusSeconds(1800)))
        val nineThirty = monday.atTime(9, 30).atZone(zone).toInstant()

        val slots = calculator.freeSlots(startOfDay(monday), 30, booked, monday, monday)

        assertTrue(slots.contains(nineThirty))
    }
}
