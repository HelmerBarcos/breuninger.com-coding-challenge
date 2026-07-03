package com.breuninger.homefeed.feed.domain

import java.time.Instant

@JvmInline
value class BngrModuleId(val value: String)

@JvmInline
value class BngrProductId(val value: String)

/** The feed's own view of a product — providers map their domain entities into this. */
data class BngrFeedProduct(
    val id: BngrProductId,
    val name: String,
    val brand: String,
    val priceCents: Int,
    val imageUrl: String,
)

/** The feed's own view of a past purchase. */
data class BngrFeedPurchase(
    val productName: String,
    val priceCents: Int,
    val purchasedAt: Instant,
)

/**
 * One renderable unit of the homefeed. Sealed on purpose: adding a module type
 * forces every exhaustive `when` (e.g. the DTO mapper) to handle it at compile time.
 */
sealed interface BngrFeedModule {
    val id: BngrModuleId
}

/**
 * Carries identity data only — never a composed salutation ("Guten Morgen, ...").
 * Time-of-day and locale belong to the client. See ADR-008.
 */
data class BngrGreetingModule(
    val firstName: String?,
    val lastName: String?,
    override val id: BngrModuleId = BngrModuleId("greeting"),
) : BngrFeedModule

data class BngrSaleBannerModule(
    val headline: String,
    val ctaLabel: String,
    val imageUrl: String,
    override val id: BngrModuleId = BngrModuleId("sale-banner"),
) : BngrFeedModule

data class BngrProductTeaserModule(
    val headline: String,
    val products: List<BngrFeedProduct>,
    override val id: BngrModuleId = BngrModuleId("product-teaser"),
) : BngrFeedModule

data class BngrRecommendationsModule(
    val headline: String,
    val products: List<BngrFeedProduct>,
    override val id: BngrModuleId = BngrModuleId("recommendations"),
) : BngrFeedModule

data class BngrOrderHistoryModule(
    val purchases: List<BngrFeedPurchase>,
    override val id: BngrModuleId = BngrModuleId("order-history"),
) : BngrFeedModule
