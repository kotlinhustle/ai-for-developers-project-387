package com.example.backend.config

import com.example.backend.booking.BookingConflictException
import com.example.backend.booking.InvalidBookingException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ValidationExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ProblemDetail {
        val detail =
            exception.bindingResult.fieldErrors.joinToString("; ") { error ->
                "${error.field}: ${error.defaultMessage}"
            }
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", detail)
    }

    @ExceptionHandler(InvalidBookingException::class)
    fun handleInvalidBooking(exception: InvalidBookingException): ProblemDetail =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid booking", exception.message)

    @ExceptionHandler(BookingConflictException::class)
    fun handleBookingConflict(exception: BookingConflictException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Conflict", exception.message)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(exception: DataIntegrityViolationException): ProblemDetail {
        val isOverlap = exception.mostSpecificCause.message?.contains("bookings_no_overlap") == true
        if (!isOverlap) {
            throw exception
        }
        return problem(HttpStatus.CONFLICT, "Conflict", "Slot is already booked")
    }

    private fun problem(
        status: HttpStatus,
        title: String,
        detail: String?,
    ): ProblemDetail {
        val problem = ProblemDetail.forStatus(status)
        problem.title = title
        problem.detail = detail
        return problem
    }
}
