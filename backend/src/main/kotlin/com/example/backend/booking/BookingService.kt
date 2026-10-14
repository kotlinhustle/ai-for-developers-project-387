package com.example.backend.booking

import com.example.backend.meetingtypes.MeetingTypeRepository
import com.example.backend.slots.TimeRange
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Service
class BookingService(
    private val meetingTypeRepository: MeetingTypeRepository,
    private val bookingRepository: BookingRepository,
    private val bookingPolicy: BookingPolicy,
    private val clock: Clock,
) {
    fun upcoming(): List<BookingEntity> =
        bookingRepository.findByStartAtGreaterThanEqualOrderByStartAtAsc(
            clock.instant().atOffset(ZoneOffset.UTC),
        )

    fun book(
        meetingTypeId: UUID,
        start: Instant,
        guestName: String,
        guestEmail: String,
        comment: String?,
    ): BookingResult {
        val meetingType =
            meetingTypeRepository.findById(meetingTypeId).orElse(null)
                ?: return BookingResult.UnknownMeetingType

        val end = start.plus(Duration.ofMinutes(meetingType.durationMinutes.toLong()))
        val booked =
            bookingRepository
                .findOverlapping(start.atOffset(ZoneOffset.UTC), end.atOffset(ZoneOffset.UTC))
                .map { TimeRange(it.startAt.toInstant(), it.endAt.toInstant()) }

        return when (val validation = bookingPolicy.validate(clock.instant(), meetingType.durationMinutes, start, booked)) {
            is BookingValidation.Invalid -> BookingResult.Invalid(validation.reason)
            BookingValidation.Conflict -> BookingResult.Conflict
            BookingValidation.Valid -> {
                val saved =
                    bookingRepository.save(
                        BookingEntity(
                            id = UUID.randomUUID(),
                            meetingTypeId = meetingTypeId,
                            startAt = start.atOffset(ZoneOffset.UTC),
                            endAt = end.atOffset(ZoneOffset.UTC),
                            guestName = guestName,
                            guestEmail = guestEmail,
                            comment = comment,
                        ),
                    )
                BookingResult.Created(saved)
            }
        }
    }
}
