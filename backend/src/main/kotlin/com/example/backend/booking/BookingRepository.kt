package com.example.backend.booking

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface BookingRepository : JpaRepository<BookingEntity, UUID> {
    @Query("select b from BookingEntity b where b.startAt < :to and b.endAt > :from")
    fun findOverlapping(
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
    ): List<BookingEntity>

    fun findByStartAtGreaterThanEqualOrderByStartAtAsc(start: OffsetDateTime): List<BookingEntity>
}
