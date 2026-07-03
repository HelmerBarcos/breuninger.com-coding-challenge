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
import org.springframework.util.LinkedMultiValueMap
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The full e2e story: register -> login -> personalized feed with the
 * protected order_history module. Also proves the Flyway-seeded demo users
 * (ADR-009) can authenticate with the documented demo password.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["homefeed.simulation.latency=10ms"],
)
@Import(BngrTestcontainersConfiguration::class)
class BngrAuthFlowIT(@Autowired private val rest: TestRestTemplate) {

    private fun feedWithToken(token: String?): JsonNode {
        val headers = HttpHeaders().apply { token?.let { setBearerAuth(it) } }
        val response = rest.exchange("/api/v1/homefeed", HttpMethod.GET, HttpEntity<Void>(headers), JsonNode::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        return response.body!!
    }

    @Test
    fun `register then login then fetch the personalized feed`() {
        val registration = mapOf(
            "email" to "anna.muster@example.com",
            "password" to "super-secret-1",
            "firstName" to "Anna",
            "lastName" to "Muster",
            "gender" to "FEMALE",
        )
        val registered = rest.postForEntity("/api/v1/auth/register", registration, JsonNode::class.java)
        assertEquals(HttpStatus.CREATED, registered.statusCode)
        assertEquals("FEMALE", registered.body!!.get("gender").asText())

        val login = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "anna.muster@example.com", "password" to "super-secret-1"),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, login.statusCode)
        val token = login.body!!.get("accessToken").asText()

        val feed = feedWithToken(token)
        val types = feed.get("modules").map { it.get("type").asText() }
        assertEquals(listOf("greeting", "sale_banner", "product_teaser", "recommendations", "order_history"), types)

        val greeting = feed.get("modules").first { it.get("type").asText() == "greeting" }
        assertEquals("Anna", greeting.get("firstName").asText())
        assertEquals("Muster", greeting.get("lastName").asText())

        val purchases = feed.get("modules").first { it.get("type").asText() == "order_history" }.get("purchases")
        assertTrue(purchases.size() in 3..5, "registration must seed 3-5 mock purchases, got ${purchases.size()}")
    }

    @Test
    fun `flyway-seeded demo user logs in with the demo password and owns purchases`() {
        val login = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "felix.junghans@breuninger.de", "password" to "breuninger-demo"),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, login.statusCode)

        val feed = feedWithToken(login.body!!.get("accessToken").asText())
        val greeting = feed.get("modules").first { it.get("type").asText() == "greeting" }
        assertEquals("Felix", greeting.get("firstName").asText())
        assertEquals("Junghans", greeting.get("lastName").asText())

        val history = feed.get("modules").first { it.get("type").asText() == "order_history" }
        assertEquals(4, history.get("purchases").size())
    }

    @Test
    fun `oauth2-shaped token endpoint issues live tokens for the interactive docs`() {
        val form = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }
        val body = LinkedMultiValueMap<String, String>().apply {
            add("username", "helmer.barcos@breuninger.de")
            add("password", "breuninger-demo")
        }

        val response = rest.postForEntity("/api/v1/auth/token", HttpEntity(body, form), JsonNode::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Bearer", response.body!!.get("token_type").asText())
        val token = response.body!!.get("access_token").asText()

        val types = feedWithToken(token).get("modules").map { it.get("type").asText() }
        assertTrue("order_history" in types)
    }

    @Test
    fun `anonymous feed has no order_history and a tampered token is rejected`() {
        val anonymousTypes = feedWithToken(null).get("modules").map { it.get("type").asText() }
        assertTrue("order_history" !in anonymousTypes)

        val headers = HttpHeaders().apply { setBearerAuth("not-a-real-token") }
        val tampered = rest.exchange("/api/v1/homefeed", HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, tampered.statusCode)
    }

    @Test
    fun `registering an already used email answers 409 problem detail`() {
        val registration = mapOf(
            "email" to "helmer.barcos@breuninger.de",
            "password" to "irrelevant-123",
            "firstName" to "Helmer",
            "lastName" to "Barcos",
            "gender" to "MALE",
        )

        val response = rest.postForEntity("/api/v1/auth/register", registration, JsonNode::class.java)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(409, response.body!!.get("status").asInt())
    }

    @Test
    fun `wrong password answers 401 without leaking which part was wrong`() {
        val response = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "felix.junghans@breuninger.de", "password" to "wrong-password"),
            JsonNode::class.java,
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
