package com.breuninger.homefeed.orders

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

/**
 * Every user owns purchases from day one - there are no purchase endpoints, so
 * registration seeds 3-5 mocked ones (same invariant as the Flyway demo users).
 */
@Component
class BngrMockPurchaseFactory {

    fun createFor(userEmail: String): List<BngrPurchaseEntity> {
        val count = Random.nextInt(MIN_PURCHASES, MAX_PURCHASES + 1)
        return MOCK_ARTICLES.shuffled().take(count).mapIndexed { index, (name, priceCents) ->
            BngrPurchaseEntity(
                id = UUID.randomUUID(),
                userEmail = userEmail,
                productName = name,
                priceCents = priceCents,
                purchasedAt = Instant.now().minus((index + 1) * 7L, ChronoUnit.DAYS),
            )
        }
    }

    companion object {
        const val MIN_PURCHASES = 3
        const val MAX_PURCHASES = 5

        private val MOCK_ARTICLES = listOf(
            "Cashmere Scarf" to 12900,
            "Leather Chelsea Boots" to 24900,
            "Silk Blouse" to 15900,
            "Wool Overcoat" to 44900,
            "Suede Loafers" to 19900,
            "Linen Shirt" to 8900,
            "Denim Jacket" to 13900,
        )
    }
}
