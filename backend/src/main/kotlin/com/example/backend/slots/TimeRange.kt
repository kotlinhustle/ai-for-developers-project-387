package com.example.backend.slots

import java.time.Instant

data class TimeRange(
    val start: Instant,
    val end: Instant,
) {
    fun overlaps(other: TimeRange): Boolean = start < other.end && other.start < end
}
