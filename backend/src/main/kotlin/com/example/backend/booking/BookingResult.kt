package com.example.backend.booking

sealed interface BookingResult {
    data class Created(
        val booking: BookingEntity,
    ) : BookingResult

    data class Invalid(
        val reason: String,
    ) : BookingResult

    data object Conflict : BookingResult

    data object UnknownMeetingType : BookingResult
}
