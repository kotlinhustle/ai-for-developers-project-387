package com.example.backend.booking

import com.example.backend.api.BookingsApi
import com.example.backend.api.model.Booking
import com.example.backend.api.model.BookingCreate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BookingsController(
    private val bookingService: BookingService,
) : BookingsApi {
    override fun bookingsList(): ResponseEntity<List<Booking>> = ResponseEntity.ok(bookingService.upcoming().map(BookingEntity::toApiModel))

    override fun bookingsCreate(bookingCreate: BookingCreate): ResponseEntity<Booking> {
        val meetingTypeId =
            runCatching { UUID.fromString(bookingCreate.meetingTypeId) }.getOrNull()
                ?: throw InvalidBookingException("invalid meeting type id")

        val result =
            bookingService.book(
                meetingTypeId = meetingTypeId,
                start = bookingCreate.start.toInstant(),
                guestName = bookingCreate.guestName,
                guestEmail = bookingCreate.guestEmail,
                comment = bookingCreate.comment,
            )

        return when (result) {
            is BookingResult.Created -> ResponseEntity.status(HttpStatus.CREATED).body(result.booking.toApiModel())
            is BookingResult.Invalid -> throw InvalidBookingException(result.reason)
            BookingResult.Conflict -> throw BookingConflictException()
            BookingResult.UnknownMeetingType -> throw InvalidBookingException("unknown meeting type")
        }
    }
}

private fun BookingEntity.toApiModel(): Booking =
    Booking(
        id = id.toString(),
        meetingTypeId = meetingTypeId.toString(),
        start = startAt,
        end = endAt,
        guestName = guestName,
        guestEmail = guestEmail,
        comment = comment,
    )
