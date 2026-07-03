package com.breuninger.homefeed.feed.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

/**
 * Wire-format discriminators. snake_case on purpose: the `type` value is a
 * long-lived contract with the mobile clients (ADR-003).
 */
object BngrModuleTypes {
    const val GREETING = "greeting"
    const val SALE_BANNER = "sale_banner"
    const val PRODUCT_TEASER = "product_teaser"
    const val RECOMMENDATIONS = "recommendations"
    const val ORDER_HISTORY = "order_history"
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(BngrGreetingModuleDto::class, name = BngrModuleTypes.GREETING),
    JsonSubTypes.Type(BngrSaleBannerModuleDto::class, name = BngrModuleTypes.SALE_BANNER),
    JsonSubTypes.Type(BngrProductTeaserModuleDto::class, name = BngrModuleTypes.PRODUCT_TEASER),
    JsonSubTypes.Type(BngrRecommendationsModuleDto::class, name = BngrModuleTypes.RECOMMENDATIONS),
    JsonSubTypes.Type(BngrOrderHistoryModuleDto::class, name = BngrModuleTypes.ORDER_HISTORY),
)
sealed interface BngrFeedModuleDto {
    val id: String
}

/**
 * Identity data only, deliberately no composed salutation ("Guten Morgen, ..."):
 * the salutation depends on the client's local time and locale, which the server
 * does not know — the app composes it. Both names are null for anonymous callers.
 * See ADR-008.
 */
data class BngrGreetingModuleDto(
    override val id: String,
    val firstName: String?,
    val lastName: String?,
) : BngrFeedModuleDto

data class BngrSaleBannerModuleDto(
    override val id: String,
    val headline: String,
    val ctaLabel: String,
    val imageUrl: String,
) : BngrFeedModuleDto

data class BngrProductTeaserModuleDto(
    override val id: String,
    val headline: String,
    val products: List<BngrProductDto>,
) : BngrFeedModuleDto

data class BngrRecommendationsModuleDto(
    override val id: String,
    val headline: String,
    val products: List<BngrProductDto>,
) : BngrFeedModuleDto

data class BngrOrderHistoryModuleDto(
    override val id: String,
    val purchases: List<BngrPurchaseDto>,
) : BngrFeedModuleDto

data class BngrProductDto(
    val id: String,
    val name: String,
    val brand: String,
    val priceCents: Int,
    val imageUrl: String,
)

data class BngrPurchaseDto(
    val productName: String,
    val priceCents: Int,
    val purchasedAt: Instant,
)
