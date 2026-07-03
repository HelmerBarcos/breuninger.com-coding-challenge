package com.breuninger.homefeed.feed.api

import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedModule
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.Instant
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The `type` discriminator is the contract with the mobile clients (ADR-003) —
 * every module type asserts its wire value here. One parameterized case per
 * sealed subtype: adding a module type without extending this source is caught
 * by the count assertion below.
 */
@JsonTest
class BngrFeedModuleSerializationTest(@Autowired private val objectMapper: ObjectMapper) {

    @ParameterizedTest(name = "{1}")
    @MethodSource("discriminators")
    fun `every module type serializes with its snake_case discriminator`(module: BngrFeedModule, expectedType: String) {
        val json = objectMapper.readTree(objectMapper.writeValueAsString(module.toDto()))

        assertEquals(expectedType, json.get("type").asText())
    }

    @Test
    fun `the discriminator test covers every sealed module type`() {
        assertEquals(
            BngrFeedModule::class.sealedSubclasses.size,
            discriminators().count().toInt(),
            "a new BngrFeedModule subtype must get a discriminator case here",
        )
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

    companion object {
        private val product = BngrFeedProduct(BngrProductId("p-1"), "Wool Coat", "BOSS", 44900, "https://img")

        @JvmStatic
        fun discriminators(): Stream<Arguments> = Stream.of(
            Arguments.of(BngrGreetingModule("Anna", "Muster"), "greeting"),
            Arguments.of(BngrSaleBannerModule("Sale", "Shop", "img"), "sale_banner"),
            Arguments.of(BngrProductTeaserModule("New", listOf(product)), "product_teaser"),
            Arguments.of(BngrRecommendationsModule("For you", listOf(product)), "recommendations"),
            Arguments.of(BngrOrderHistoryModule(listOf(BngrFeedPurchase("Coat", 100, Instant.EPOCH))), "order_history"),
        )
    }
}
