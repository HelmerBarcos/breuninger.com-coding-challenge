package com.breuninger.homefeed

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.util.stream.Stream
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

    /** One strictness rule per row; each rejected request answers exactly one typed error. */
    @ParameterizedTest(name = "{0} -> {2} {3}")
    @MethodSource("singleErrorCases")
    fun `strict api answers a single typed error`(
        name: String,
        call: BngrApiCall,
        expectedStatus: HttpStatus,
        expectedCode: String,
        expectedField: String?,
    ) {
        val response = call.execute(rest)

        assertEquals(expectedStatus, response.statusCode)
        assertEquals(expectedStatus.value(), response.body!!.get("status").asInt())
        assertEquals(listOf(expectedCode), codesOf(response.body!!))
        expectedField?.let { assertEquals(setOf(it), fieldsOf(response.body!!)) }
    }

    @Test
    fun `unknown query parameters are rejected - one error per offending parameter`() {
        val response = rest.getForEntity("/api/v1/homefeed?foo=1&bar=2", JsonNode::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(listOf("UNKNOWN_PARAMETER", "UNKNOWN_PARAMETER"), codesOf(response.body!!))
        assertEquals(setOf("foo", "bar"), fieldsOf(response.body!!))
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

    /** SAM interface so heterogeneous requests fit one parameterized signature. */
    fun interface BngrApiCall {
        fun execute(rest: TestRestTemplate): ResponseEntity<JsonNode>
    }

    companion object {
        private fun jsonHeaders() = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        @JvmStatic
        fun singleErrorCases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "unknown path",
                BngrApiCall { it.getForEntity("/api/v1/does-not-exist", JsonNode::class.java) },
                HttpStatus.NOT_FOUND, "NOT_FOUND", null,
            ),
            Arguments.of(
                "wrong http method",
                BngrApiCall { it.exchange("/api/v1/homefeed", HttpMethod.DELETE, HttpEntity<Void>(HttpHeaders()), JsonNode::class.java) },
                HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", null,
            ),
            Arguments.of(
                "malformed json body",
                BngrApiCall { it.postForEntity("/api/v1/auth/login", HttpEntity("{not json", jsonHeaders()), JsonNode::class.java) },
                HttpStatus.BAD_REQUEST, "MALFORMED_BODY", null,
            ),
            Arguments.of(
                "unknown body field",
                BngrApiCall {
                    it.postForEntity(
                        "/api/v1/auth/login",
                        mapOf("email" to "felix.junghans@breuninger.de", "password" to "breuninger-demo", "remember" to true),
                        JsonNode::class.java,
                    )
                },
                HttpStatus.BAD_REQUEST, "UNKNOWN_FIELD", "remember",
            ),
            Arguments.of(
                "invalid enum value",
                BngrApiCall {
                    it.postForEntity(
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
                },
                HttpStatus.BAD_REQUEST, "FIELD_INVALID", "gender",
            ),
        )

        private fun codesOf(body: JsonNode): List<String> = body.get("errors").map { it.get("code").asText() }

        private fun fieldsOf(body: JsonNode): Set<String> = body.get("errors").mapNotNull { it.get("field")?.takeIf { f -> !f.isNull }?.asText() }.toSet()
    }

    private fun codesOf(body: JsonNode): List<String> = Companion.codesOf(body)

    private fun fieldsOf(body: JsonNode): Set<String> = Companion.fieldsOf(body)
}
