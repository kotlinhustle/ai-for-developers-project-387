package com.example.backend.meetingtypes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "meeting_types")
class MeetingTypeEntity(
    @Id
    val id: UUID,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false, length = 1000)
    var description: String,
    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,
)
