package com.example.backend.meetingtypes

import com.example.backend.booking.BookingRepository
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
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MeetingTypesApiTest : PostgresTestSupport() {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val rest = RestClient.create()

    @BeforeEach
    fun cleanDatabase() {
        bookingRepository.deleteAll()
        repository.deleteAll()
    }

    @Autowired
    private lateinit var repository: MeetingTypeRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Test
    fun `GET meeting-types returns an empty list`() {
        val response = get()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("[]", response.body)
    }

    @Test
    fun `POST meeting-types creates a type and GET lists it`() {
        val created = post("""{"name":"Консультация","description":"Разбор задачи","durationMinutes":30}""")

        assertEquals(HttpStatus.CREATED, created.statusCode)
        assertTrue(created.body!!.contains("\"name\":\"Консультация\""))
        assertTrue(created.body!!.contains("\"durationMinutes\":30"))

        val listed = get()
        assertTrue(listed.body!!.contains("\"name\":\"Консультация\""))
    }

    @Test
    fun `POST meeting-types rejects an empty name with 422`() {
        val exception =
            assertThrows<RestClientResponseException> {
                post("""{"name":"","description":"Разбор","durationMinutes":30}""")
            }

        assertEquals(422, exception.statusCode.value())
    }

    @Test
    fun `POST meeting-types rejects a non-positive duration with 422`() {
        val exception =
            assertThrows<RestClientResponseException> {
                post("""{"name":"Консультация","description":"Разбор","durationMinutes":0}""")
            }

        assertEquals(422, exception.statusCode.value())
    }

    @Test
    fun `GET meeting-types slots returns free slots with the owner zone`() {
        val created = post("""{"name":"Консультация","description":"Разбор","durationMinutes":30}""")
        val id = Regex("\"id\":\"([^\"]+)\"").find(created.body!!)!!.groupValues[1]
        val nextMonday = LocalDate.now(ZoneId.of("Asia/Yerevan")).with(TemporalAdjusters.next(DayOfWeek.MONDAY))

        val response = getSlots(id, nextMonday, nextMonday)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.contains("\"zoneId\":\"Asia/Yerevan\""))
        assertTrue(response.body!!.contains("\"start\""))
    }

    @Test
    fun `GET meeting-types slots returns 404 for an unknown id`() {
        val exception =
            assertThrows<RestClientResponseException> {
                getSlots(UUID.randomUUID().toString())
            }

        assertEquals(404, exception.statusCode.value())
    }

    private fun get() =
        rest
            .get()
            .uri("http://localhost:$port/meeting-types")
            .retrieve()
            .toEntity(String::class.java)

    private fun getSlots(
        id: String,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ) = rest
        .get()
        .uri(slotsUri(id, from, to))
        .retrieve()
        .toEntity(String::class.java)

    private fun slotsUri(
        id: String,
        from: LocalDate?,
        to: LocalDate?,
    ): String {
        val base = "http://localhost:$port/meeting-types/$id/slots"
        return if (from != null && to != null) "$base?from=$from&to=$to" else base
    }

    private fun post(body: String) =
        rest
            .post()
            .uri("http://localhost:$port/meeting-types")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(String::class.java)
}
