package com.breuninger.homefeed.feed.api

import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedProduct
import com.breuninger.homefeed.feed.domain.BngrFeedPurchase
import com.breuninger.homefeed.feed.domain.BngrGreetingModule
import com.breuninger.homefeed.feed.domain.BngrOrderHistoryModule
import com.breuninger.homefeed.feed.domain.BngrProductId
import com.breuninger.homefeed.feed.domain.BngrProductTeaserModule
import com.breuninger.homefeed.feed.domain.BngrRecommendationsModule
import com.breuninger.homefeed.feed.domain.BngrSaleBannerModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The `type` discriminator is the contract with the mobile clients (ADR-003) —
 * every module type asserts its wire value here.
 */
@JsonTest
class BngrFeedModuleSerializationTest(@Autowired private val objectMapper: ObjectMapper) {

    private val product = BngrFeedProduct(BngrProductId("p-1"), "Wool Coat", "BOSS", 44900, "https://img")

    private fun typeOf(dto: BngrFeedModuleDto): String =
        objectMapper.readTree(objectMapper.writeValueAsString(dto)).get("type").asText()

    @Test
    fun `every module type serializes with its snake_case discriminator`() {
        assertEquals("greeting", typeOf(BngrGreetingModule("Anna", "Muster").toDto()))
        assertEquals("sale_banner", typeOf(BngrSaleBannerModule("Sale", "Shop", "img").toDto()))
        assertEquals("product_teaser", typeOf(BngrProductTeaserModule("New", listOf(product)).toDto()))
        assertEquals("recommendations", typeOf(BngrRecommendationsModule("For you", listOf(product)).toDto()))
        assertEquals("order_history", typeOf(BngrOrderHistoryModule(listOf(BngrFeedPurchase("Coat", 100, Instant.EPOCH))).toDto()))
    }

    @Test
    fun `greeting carries identity data only - the salutation is composed by the client`() {
        // ADR-008: no time-of-day or locale-dependent text may leave the server
        val json = objectMapper.readTree(
            objectMapper.writeValueAsString(BngrGreetingModule("Helmer", "Barcos").toDto()),
        )

        assertEquals(setOf("type", "id", "firstName", "lastName"), json.fieldNames().asSequence().toSet())
        assertEquals("Helmer", json.get("firstName").asText())
        assertEquals("Barcos", json.get("lastName").asText())
    }

    @Test
    fun `anonymous greeting serializes explicit nulls for the names`() {
        val module = BngrGreetingModule(
            firstName = BngrFeedContext.ANONYMOUS.user?.firstName,
            lastName = BngrFeedContext.ANONYMOUS.user?.lastName,
        )

        val json = objectMapper.readTree(objectMapper.writeValueAsString(module.toDto()))

        assertTrue(json.get("firstName").isNull)
        assertTrue(json.get("lastName").isNull)
    }
}
