package com.breuninger.homefeed

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The error contract (ADR-010): every error response is an RFC 9457 problem
 * with status + detail + an errors[] array of {code, message, field?} — codes
 * are what the client UI renders on, and one response can carry several errors.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["homefeed.simulation.latency=10ms"],
)
@Import(BngrTestcontainersConfiguration::class)
class BngrErrorContractIT(@Autowired private val rest: TestRestTemplate) {

    private fun codesOf(body: JsonNode): List<String> = body.get("errors").map { it.get("code").asText() }

    private fun fieldsOf(body: JsonNode): Set<String> = body.get("errors").mapNotNull { it.get("field")?.asText() }.toSet()

    @Test
    fun `unknown paths answer 404 with the shared error shape`() {
        val response = rest.getForEntity("/api/v1/does-not-exist", JsonNode::class.java)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body!!.get("status").asInt())
        assertEquals(listOf("NOT_FOUND"), codesOf(response.body!!))
    }

    @Test
    fun `unknown query parameters are rejected - one error per offending parameter`() {
        val response = rest.getForEntity("/api/v1/homefeed?foo=1&bar=2", JsonNode::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(listOf("UNKNOWN_PARAMETER", "UNKNOWN_PARAMETER"), codesOf(response.body!!))
        assertEquals(setOf("foo", "bar"), fieldsOf(response.body!!))
    }

    @Test
    fun `unknown body fields are rejected instead of silently ignored`() {
        val response = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "felix.junghans@breuninger.de", "password" to "breuninger-demo", "remember" to true),
            JsonNode::class.java,
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(listOf("UNKNOWN_FIELD"), codesOf(response.body!!))
        assertEquals(setOf("remember"), fieldsOf(response.body!!))
    }

    @Test
    fun `several validation failures come back in a single response`() {
        val response = rest.postForEntity(
            "/api/v1/auth/register",
            mapOf(
                "email" to "not-an-email",
                "password" to "short",
                "firstName" to "Anna",
                "lastName" to "Muster",
                "gender" to "FEMALE",
            ),
            JsonNode::class.java,
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertTrue(codesOf(response.body!!).all { it == "FIELD_INVALID" })
        assertEquals(setOf("email", "password"), fieldsOf(response.body!!))
    }

    @Test
    fun `invalid enum value points at the offending field`() {
        val response = rest.postForEntity(
            "/api/v1/auth/register",
            mapOf(
                "email" to "someone@example.com",
                "password" to "long-enough-1",
                "firstName" to "Some",
                "lastName" to "One",
                "gender" to "OTHER",
            ),
            JsonNode::class.java,
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(listOf("FIELD_INVALID"), codesOf(response.body!!))
        assertEquals(setOf("gender"), fieldsOf(response.body!!))
    }

    @Test
    fun `wrong http method answers 405 with the allowed methods`() {
        val response = rest.exchange("/api/v1/homefeed", HttpMethod.DELETE, HttpEntity<Void>(HttpHeaders()), JsonNode::class.java)

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.statusCode)
        assertEquals(listOf("METHOD_NOT_ALLOWED"), codesOf(response.body!!))
    }

    @Test
    fun `malformed json answers 400 with MALFORMED_BODY`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val response = rest.postForEntity("/api/v1/auth/login", HttpEntity("{not json", headers), JsonNode::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(listOf("MALFORMED_BODY"), codesOf(response.body!!))
    }

    @Test
    fun `messages stay english even when the client asks for another language`() {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(HttpHeaders.ACCEPT_LANGUAGE, "de-DE")
        }
        val body = """{"email":"not-an-email","password":"long-enough-1","firstName":"A","lastName":"B","gender":"FEMALE"}"""

        val response = rest.postForEntity("/api/v1/auth/register", HttpEntity(body, headers), JsonNode::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val message = response.body!!.get("errors").first().get("message").asText()
        assertEquals("must be a well-formed email address", message)
    }

    @Test
    fun `error responses use the problem+json media type`() {
        val response = rest.getForEntity("/api/v1/does-not-exist", String::class.java)

        assertTrue(response.headers.contentType.toString().startsWith("application/problem+json"))
    }
}
