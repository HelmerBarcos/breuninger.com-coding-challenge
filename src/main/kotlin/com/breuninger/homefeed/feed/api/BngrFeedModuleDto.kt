package com.breuninger.homefeed.feed.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
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
@Schema(
    description = "One renderable unit of the homefeed. The `type` field tells the app which " +
        "module to render; unknown types must be skipped by clients so the feed can grow " +
        "without breaking older app versions.",
)
sealed interface BngrFeedModuleDto

/**
 * Identity data only, deliberately no composed salutation ("Guten Morgen, ..."):
 * the salutation depends on the client's local time and locale, which the server
 * does not know - the app composes it. Both names are null for anonymous callers.
 * See ADR-008.
 */
@Schema(
    description = "Personal greeting slot, always first in the feed. Carries identity data only - " +
        "the app composes the localized, time-of-day salutation itself (the server cannot know " +
        "the client's clock or locale). Both names are null for anonymous callers: render a " +
        "generic greeting then.",
)
data class BngrGreetingModuleDto(
    @get:Schema(description = "First name of the authenticated user, or null when anonymous.", example = "Anna")
    val firstName: String?,
    @get:Schema(description = "Last name of the authenticated user, or null when anonymous.", example = "Muster")
    val lastName: String?,
) : BngrFeedModuleDto

@Schema(description = "Marketing banner for the currently active sale campaign. Absent from the feed when no campaign is active.")
data class BngrSaleBannerModuleDto(
    @get:Schema(example = "Mid-Season Sale - bis zu 30%")
    val headline: String,
    @get:Schema(description = "Label for the call-to-action button.", example = "Jetzt shoppen")
    val ctaLabel: String,
    @get:Schema(description = "Banner artwork to display.")
    val imageUrl: String,
) : BngrFeedModuleDto

@Schema(description = "Small grid of curated products (e.g. new arrivals).")
data class BngrProductTeaserModuleDto(
    @get:Schema(example = "Neu bei Breuninger")
    val headline: String,
    val products: List<BngrProductDto>,
) : BngrFeedModuleDto

@Schema(description = "Personalized product picks. Currently a mocked selection; the contract will not change when a real recommendation backend is plugged in.")
data class BngrRecommendationsModuleDto(
    @get:Schema(example = "Für dich empfohlen")
    val headline: String,
    val products: List<BngrProductDto>,
) : BngrFeedModuleDto

@Schema(description = "The authenticated user's most recent purchases, newest first. Only present when the request carries a valid Bearer token.")
data class BngrOrderHistoryModuleDto(
    val purchases: List<BngrPurchaseDto>,
) : BngrFeedModuleDto

@Schema(description = "A product as rendered inside a feed module.")
data class BngrProductDto(
    val id: String,
    @get:Schema(example = "Double-Breasted Wool Coat")
    val name: String,
    @get:Schema(example = "BOSS")
    val brand: String,
    @get:Schema(description = "Price in euro cents - clients format the currency.", example = "44900")
    val priceCents: Int,
    val imageUrl: String,
)

@Schema(description = "One past purchase. Product name and price are snapshots taken at purchase time.")
data class BngrPurchaseDto(
    @get:Schema(example = "Leather Chelsea Boots")
    val productName: String,
    @get:Schema(description = "Paid price in euro cents.", example = "34900")
    val priceCents: Int,
    val purchasedAt: Instant,
)
