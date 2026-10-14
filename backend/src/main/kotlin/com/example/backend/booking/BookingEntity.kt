package com.example.backend.booking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "bookings")
class BookingEntity(
    @Id
    val id: UUID,
    @Column(name = "meeting_type_id", nullable = false)
    val meetingTypeId: UUID,
    @Column(name = "start_at", nullable = false)
    val startAt: OffsetDateTime,
    @Column(name = "end_at", nullable = false)
    val endAt: OffsetDateTime,
    @Column(name = "guest_name", nullable = false)
    var guestName: String,
    @Column(name = "guest_email", nullable = false)
    var guestEmail: String,
    @Column(name = "comment")
    var comment: String?,
)
