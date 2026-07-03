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
        assertEquals("Mid-Season Sale - bis zu 30%", banner.get("headline").asText())
        assertEquals("Jetzt shoppen", banner.get("ctaLabel").asText())

        val teaser = modules.first { it.get("type").asText() == "product_teaser" }
        assertEquals(4, teaser.get("products").size())
    }

    @Test
    fun `produced dtos respect the model constraints`() {
        // the wire payload never ships broken links, unbounded strings or negative prices
        val modules = rest.getForEntity("/api/v1/homefeed", JsonNode::class.java).body!!.get("modules")

        val products = modules.filter { it.has("products") }.flatMap { it.get("products") }
        assertTrue(products.isNotEmpty())
        products.forEach { product ->
            val url = java.net.URI(product.get("imageUrl").asText())
            assertTrue(url.scheme == "https", "image links must be absolute https URLs, got: $url")
            assertTrue(product.get("name").asText().length in 1..255)
            assertTrue(product.get("brand").asText().length in 1..100)
            assertTrue(product.get("priceCents").asInt() >= 0)
        }

        val banner = modules.first { it.get("type").asText() == "sale_banner" }
        assertTrue(java.net.URI(banner.get("imageUrl").asText()).scheme == "https")
        assertTrue(banner.get("headline").asText().length in 1..255)
    }

    @Test
    fun `module content follows accept-language with german as the default`() {
        fun headlineOf(body: JsonNode, type: String) =
            body.get("modules").first { it.get("type").asText() == type }.get("headline").asText()

        fun feedWithLanguage(language: String?): JsonNode {
            val headers = org.springframework.http.HttpHeaders()
            language?.let { headers.set(org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE, it) }
            return rest.exchange(
                "/api/v1/homefeed",
                org.springframework.http.HttpMethod.GET,
                org.springframework.http.HttpEntity<Void>(headers),
                JsonNode::class.java,
            ).body!!
        }

        // no header -> German, the shop's default (ADR-011)
        assertEquals("Neu bei Breuninger", headlineOf(feedWithLanguage(null), "product_teaser"))

        val english = feedWithLanguage("en-US,en;q=0.9")
        assertEquals("New at Breuninger", headlineOf(english, "product_teaser"))
        assertEquals("Recommended for you", headlineOf(english, "recommendations"))

        // unsupported language degrades to German instead of failing
        assertEquals("Für dich empfohlen", headlineOf(feedWithLanguage("fr-FR"), "recommendations"))
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

    @Test
    fun `spec documents feed auth as optional with anonymous and authenticated examples`() {
        val spec = rest.getForEntity("/v3/api-docs", JsonNode::class.java).body!!
        val feedGet = spec.get("paths").get("/api/v1/homefeed").get("get")

        // an EMPTY requirement in the security list = callable without any Authorization header
        val security = feedGet.get("security")
        assertTrue(security.any { it.isEmpty }, "security list must contain the empty (anonymous) requirement")
        assertTrue(security.any { it.has("bearerAuth") })
        assertTrue(security.any { it.has("oauthPassword") })

        val examples = feedGet.get("responses").get("200").get("content").get("application/json").get("examples")
        assertTrue(examples.has("anonymous") && examples.has("authenticated"))

        // the live-login flow interactive docs use
        val tokenUrl = spec.get("components").get("securitySchemes").get("oauthPassword")
            .get("flows").get("password").get("tokenUrl").asText()
        assertEquals("/api/v1/auth/token", tokenUrl)
    }
}
