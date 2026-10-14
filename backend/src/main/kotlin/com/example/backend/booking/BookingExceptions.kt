package com.example.backend.booking

class BookingConflictException : RuntimeException("Slot is already booked")

class InvalidBookingException(
    message: String,
) : RuntimeException(message)
