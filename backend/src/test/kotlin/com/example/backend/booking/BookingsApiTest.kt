package com.example.backend.booking

import com.example.backend.meetingtypes.MeetingTypeRepository
import com.example.backend.support.PostgresTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingsApiTest : PostgresTestSupport() {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @Autowired
    private lateinit var meetingTypeRepository: MeetingTypeRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    private val rest = RestClient.create()
    private val zone: ZoneId = ZoneId.of("Asia/Yerevan")
    private val nextMonday: LocalDate = LocalDate.now(zone).with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    @BeforeEach
    fun cleanDatabase() {
        bookingRepository.deleteAll()
        meetingTypeRepository.deleteAll()
    }

    @Test
    fun `creates a booking for a free slot`() {
        val meetingTypeId = createMeetingType("Консультация", 30)

        val response = postBooking(meetingTypeId, startAt(9, 0))

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertTrue(response.body!!.contains("\"guestName\":\"Гость\""))
        assertEquals(1, bookingRepository.count())
    }

    @Test
    fun `rejects an overlapping booking with 409 across meeting types`() {
        val first = createMeetingType("Первый", 30)
        val second = createMeetingType("Второй", 30)
        postBooking(first, startAt(9, 0))

        val exception =
            assertThrows<RestClientResponseException> {
                postBooking(second, startAt(9, 0))
            }

        assertEquals(409, exception.statusCode.value())
    }

    @Test
    fun `rejects a slot outside the grid with 422`() {
        val meetingTypeId = createMeetingType("Консультация", 30)

        val exception =
            assertThrows<RestClientResponseException> {
                postBooking(meetingTypeId, startAt(9, 15))
            }

        assertEquals(422, exception.statusCode.value())
    }

    @Test
    fun `rejects an empty guest name with 422`() {
        val meetingTypeId = createMeetingType("Консультация", 30)

        val exception =
            assertThrows<RestClientResponseException> {
                postBooking(meetingTypeId, startAt(9, 0), guestName = "")
            }

        assertEquals(422, exception.statusCode.value())
    }

    @Test
    fun `rejects an empty guest email with 422`() {
        val meetingTypeId = createMeetingType("Консультация", 30)

        val exception =
            assertThrows<RestClientResponseException> {
                postBooking(meetingTypeId, startAt(9, 0), guestEmail = "")
            }

        assertEquals(422, exception.statusCode.value())
    }

    @Test
    fun `rejects a slot outside working hours with 422`() {
        val meetingTypeId = createMeetingType("Консультация", 30)

        val exception =
            assertThrows<RestClientResponseException> {
                postBooking(meetingTypeId, startAt(18, 0))
            }

        assertEquals(422, exception.statusCode.value())
    }

    @Test
    fun `lists upcoming bookings of all types sorted by start`() {
        val first = createMeetingType("Первый", 30)
        val second = createMeetingType("Второй", 30)
        postBooking(first, startAt(11, 0))
        postBooking(second, startAt(9, 0))

        val body = getBookings().body!!

        assertTrue(body.contains("\"meetingTypeId\":\"$first\""))
        assertTrue(body.contains("\"meetingTypeId\":\"$second\""))
        assertTrue(body.indexOf("05:00:00Z") < body.indexOf("07:00:00Z"))
    }

    @Test
    fun `excludes past bookings from the list`() {
        val meetingTypeId = createMeetingType("Консультация", 30)
        val pastStart = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        bookingRepository.save(
            BookingEntity(
                id = UUID.randomUUID(),
                meetingTypeId = UUID.fromString(meetingTypeId),
                startAt = pastStart,
                endAt = pastStart.plusMinutes(30),
                guestName = "Гость",
                guestEmail = "guest@example.com",
                comment = null,
            ),
        )

        assertEquals("[]", getBookings().body)
    }

    @Test
    fun `allows only one of two concurrent bookings for the same slot`() {
        val meetingTypeId = createMeetingType("Консультация", 30)
        val start = startAt(10, 0)
        val barrier = CyclicBarrier(2)
        val latch = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)
        val results = IntArray(2)

        for (index in 0 until 2) {
            executor.execute {
                barrier.await()
                results[index] = postBookingStatus(meetingTypeId, start)
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(1, results.count { status -> status == 201 })
        assertEquals(1, results.count { status -> status == 409 })
    }

    private fun getBookings() =
        rest
            .get()
            .uri("http://localhost:$port/bookings")
            .retrieve()
            .toEntity(String::class.java)

    private fun postBookingStatus(
        meetingTypeId: String,
        start: String,
    ): Int =
        try {
            rest
                .post()
                .uri("http://localhost:$port/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    """{"meetingTypeId":"$meetingTypeId","start":"$start","guestName":"Гость","guestEmail":"guest@example.com"}""",
                ).retrieve()
                .toBodilessEntity()
                .statusCode
                .value()
        } catch (exception: RestClientResponseException) {
            exception.statusCode.value()
        }

    private fun startAt(
        hour: Int,
        minute: Int,
    ): String =
        nextMonday
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toString()

    private fun createMeetingType(
        name: String,
        durationMinutes: Int,
    ): String {
        val response =
            rest
                .post()
                .uri("http://localhost:$port/meeting-types")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"name":"$name","description":"Описание","durationMinutes":$durationMinutes}""")
                .retrieve()
                .toEntity(String::class.java)

        return Regex("\"id\":\"([^\"]+)\"").find(response.body!!)!!.groupValues[1]
    }

    private fun postBooking(
        meetingTypeId: String,
        start: String,
        guestName: String = "Гость",
        guestEmail: String = "guest@example.com",
    ) = rest
        .post()
        .uri("http://localhost:$port/bookings")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """{"meetingTypeId":"$meetingTypeId","start":"$start","guestName":"$guestName","guestEmail":"$guestEmail"}""",
        ).retrieve()
        .toEntity(String::class.java)
}
