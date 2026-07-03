package com.breuninger.homefeed

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Boots the real application against a real Postgres (Testcontainers) and
 * exercises the public feed contract end to end.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["homefeed.simulation.latency=10ms"],
)
@Import(BngrTestcontainersConfiguration::class)
class BngrHomefeedApiIT(@Autowired private val rest: TestRestTemplate) {

    @Test
    fun `anonymous feed returns the four public modules in order`() {
        val response = rest.getForEntity("/api/v1/homefeed", JsonNode::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        val types = body.get("modules").map { it.get("type").asText() }

        assertEquals(listOf("greeting", "sale_banner", "product_teaser", "recommendations"), types)
        assertEquals(4, body.get("meta").get("moduleCount").asInt())
        assertTrue(body.get("meta").hasNonNull("assemblyTimeMs"))
    }

    @Test
    fun `anonymous greeting has null names and the sale banner carries the seeded campaign`() {
        val modules = rest.getForEntity("/api/v1/homefeed", JsonNode::class.java).body!!.get("modules")

        val greeting = modules.first { it.get("type").asText() == "greeting" }
        assertTrue(greeting.get("firstName").isNull)
        assertTrue(greeting.get("lastName").isNull)

        val banner = modules.first { it.get("type").asText() == "sale_banner" }
        assertEquals("Mid-Season Sale – bis zu 30%", banner.get("headline").asText())
        assertEquals("Jetzt shoppen", banner.get("ctaLabel").asText())

        val teaser = modules.first { it.get("type").asText() == "product_teaser" }
        assertEquals(4, teaser.get("products").size())
    }

    @Test
    fun `liveness and readiness probes answer`() {
        assertEquals(HttpStatus.OK, rest.getForEntity("/actuator/health/liveness", String::class.java).statusCode)
        assertEquals(HttpStatus.OK, rest.getForEntity("/actuator/health/readiness", String::class.java).statusCode)
    }

    @Test
    fun `openapi spec is served for the scalar reference`() {
        val spec = rest.getForEntity("/v3/api-docs", JsonNode::class.java)

        assertEquals(HttpStatus.OK, spec.statusCode)
        assertTrue(spec.body!!.get("paths").has("/api/v1/homefeed"))
    }
}
