package com.example.backend.meetingtypes

import com.example.backend.api.MeetingTypesApi
import com.example.backend.api.model.MeetingType
import com.example.backend.api.model.MeetingTypeCreate
import com.example.backend.api.model.Slot
import com.example.backend.api.model.SlotsResponse
import com.example.backend.booking.BookingRepository
import com.example.backend.slots.OwnerSettings
import com.example.backend.slots.SlotCalculator
import com.example.backend.slots.TimeRange
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@RestController
class MeetingTypesController(
    private val repository: MeetingTypeRepository,
    private val bookingRepository: BookingRepository,
    private val slotCalculator: SlotCalculator,
    private val ownerSettings: OwnerSettings,
) : MeetingTypesApi {
    override fun meetingTypesList(): ResponseEntity<List<MeetingType>> =
        ResponseEntity.ok(repository.findAll().map(MeetingTypeEntity::toApiModel))

    override fun meetingTypesCreate(meetingTypeCreate: MeetingTypeCreate): ResponseEntity<MeetingType> {
        val saved =
            repository.save(
                MeetingTypeEntity(
                    id = UUID.randomUUID(),
                    name = meetingTypeCreate.name,
                    description = meetingTypeCreate.description,
                    durationMinutes = meetingTypeCreate.durationMinutes,
                ),
            )

        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toApiModel())
    }

    override fun meetingTypesListSlots(
        meetingTypeId: String,
        from: LocalDate?,
        to: LocalDate?,
    ): ResponseEntity<SlotsResponse> {
        val id =
            runCatching { UUID.fromString(meetingTypeId) }.getOrNull()
                ?: return ResponseEntity.notFound().build()
        val meetingType =
            repository.findById(id).orElse(null)
                ?: return ResponseEntity.notFound().build()

        val now = Instant.now()
        val booked =
            bookingRepository
                .findOverlapping(
                    now.atOffset(ZoneOffset.UTC),
                    now.plus(Duration.ofDays(15)).atOffset(ZoneOffset.UTC),
                ).map { TimeRange(it.startAt.toInstant(), it.endAt.toInstant()) }
        val slots = slotCalculator.freeSlots(now, meetingType.durationMinutes, booked, from, to)

        val response =
            SlotsResponse(
                zoneId = ownerSettings.zoneId,
                slots = slots.map { Slot(start = it.atOffset(ZoneOffset.UTC), durationMinutes = meetingType.durationMinutes) },
            )

        return ResponseEntity.ok(response)
    }
}

private fun MeetingTypeEntity.toApiModel(): MeetingType =
    MeetingType(
        id = id.toString(),
        name = name,
        description = description,
        durationMinutes = durationMinutes,
    )
