package com.breuninger.homefeed.feed.api

import com.breuninger.homefeed.feed.domain.BngrFeedModule
import com.breuninger.homefeed.feed.domain.BngrFeedProduct
import com.breuninger.homefeed.feed.domain.BngrFeedPurchase
import com.breuninger.homefeed.feed.domain.BngrGreetingModule
import com.breuninger.homefeed.feed.domain.BngrOrderHistoryModule
import com.breuninger.homefeed.feed.domain.BngrProductTeaserModule
import com.breuninger.homefeed.feed.domain.BngrRecommendationsModule
import com.breuninger.homefeed.feed.domain.BngrSaleBannerModule

/**
 * Exhaustive on the sealed hierarchy: a new module type without a DTO mapping
 * is a compile error, not a runtime surprise (ADR-002).
 */
fun BngrFeedModule.toDto(): BngrFeedModuleDto = when (this) {
    is BngrGreetingModule -> BngrGreetingModuleDto(firstName, lastName)
    is BngrSaleBannerModule -> BngrSaleBannerModuleDto(headline, ctaLabel, imageUrl)
    is BngrProductTeaserModule -> BngrProductTeaserModuleDto(headline, products.map { it.toDto() })
    is BngrRecommendationsModule -> BngrRecommendationsModuleDto(headline, products.map { it.toDto() })
    is BngrOrderHistoryModule -> BngrOrderHistoryModuleDto(purchases.map { it.toDto() })
}

private fun BngrFeedProduct.toDto() = BngrProductDto(id.value, name, brand, priceCents, imageUrl)

private fun BngrFeedPurchase.toDto() = BngrPurchaseDto(productName, priceCents, purchasedAt)
