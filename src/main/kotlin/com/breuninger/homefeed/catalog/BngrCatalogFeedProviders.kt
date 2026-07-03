package com.breuninger.homefeed.catalog

import com.breuninger.homefeed.feed.domain.BngrFeedSlots
import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedModule
import com.breuninger.homefeed.feed.domain.BngrFeedProduct
import com.breuninger.homefeed.feed.domain.BngrModuleProvider
import com.breuninger.homefeed.feed.domain.BngrProductId
import com.breuninger.homefeed.feed.domain.BngrProductTeaserModule
import com.breuninger.homefeed.feed.domain.BngrRecommendationsModule
import com.breuninger.homefeed.feed.domain.BngrSaleBannerModule
import com.breuninger.homefeed.shared.BngrBackendSimulator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.util.Locale

/** No active campaign -> no banner: the provider returns null instead of an empty module. */
@Component
class BngrSaleBannerModuleProvider(
    private val campaigns: BngrSaleCampaignRepository,
    private val simulator: BngrBackendSimulator,
) : BngrModuleProvider {

    override val order = BngrFeedSlots.SALE_BANNER

    override suspend fun provide(context: BngrFeedContext): BngrFeedModule? {
        val campaign = simulator.simulateRemoteCall { campaigns.findFirstByActiveTrue() } ?: return null
        return BngrSaleBannerModule(
            headline = localizedText(context.locale, german = campaign.headline, english = campaign.headlineEn),
            ctaLabel = localizedText(context.locale, german = campaign.ctaLabel, english = campaign.ctaLabelEn),
            imageUrl = campaign.imageUrl,
        )
    }
}

/** Data localization (ADR-011): English only when requested AND translated; German is the base. */
private fun localizedText(locale: Locale, german: String, english: String?): String =
    if (locale.language == Locale.ENGLISH.language) english ?: german else german

@Component
class BngrProductTeaserModuleProvider(
    private val products: BngrProductRepository,
    private val simulator: BngrBackendSimulator,
    @Qualifier("bngrContentMessageSource") private val contentMessages: MessageSource,
) : BngrModuleProvider {

    override val order = BngrFeedSlots.PRODUCT_TEASER

    override suspend fun provide(context: BngrFeedContext): BngrFeedModule? {
        val teaser = simulator.simulateRemoteCall { products.findByOrderByNameAsc(PageRequest.of(0, TEASER_SIZE)) }
        if (teaser.isEmpty()) return null
        return BngrProductTeaserModule(
            headline = contentMessages.getMessage("feed.product-teaser.headline", null, context.locale),
            products = teaser.map { it.toFeedProduct() },
        )
    }

    companion object {
        const val TEASER_SIZE = 4
    }
}

/**
 * Mocked personalization: serves the catalog slice after the teaser's. A real
 * implementation would call a recommendation backend with context.user.
 */
@Component
class BngrRecommendationsModuleProvider(
    private val products: BngrProductRepository,
    private val simulator: BngrBackendSimulator,
    @Qualifier("bngrContentMessageSource") private val contentMessages: MessageSource,
) : BngrModuleProvider {

    override val order = BngrFeedSlots.RECOMMENDATIONS

    override suspend fun provide(context: BngrFeedContext): BngrFeedModule? {
        val picks = simulator.simulateRemoteCall { products.findByOrderByNameAsc(PageRequest.of(1, PICKS_SIZE)) }
        if (picks.isEmpty()) return null
        return BngrRecommendationsModule(
            headline = contentMessages.getMessage("feed.recommendations.headline", null, context.locale),
            products = picks.map { it.toFeedProduct() },
        )
    }

    companion object {
        const val PICKS_SIZE = 4
    }
}

private fun BngrProductEntity.toFeedProduct() = BngrFeedProduct(
    id = BngrProductId(id.toString()),
    name = name,
    brand = brand,
    priceCents = priceCents,
    imageUrl = imageUrl,
)
