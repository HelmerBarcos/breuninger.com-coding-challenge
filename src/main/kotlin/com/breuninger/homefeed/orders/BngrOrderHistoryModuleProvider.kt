package com.breuninger.homefeed.orders

import com.breuninger.homefeed.feed.domain.BngrFeedSlots
import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedModule
import com.breuninger.homefeed.feed.domain.BngrFeedPurchase
import com.breuninger.homefeed.feed.domain.BngrModuleProvider
import com.breuninger.homefeed.feed.domain.BngrOrderHistoryModule
import com.breuninger.homefeed.shared.BngrBackendSimulator
import org.springframework.stereotype.Component

/**
 * The protected module: personal data, so it exists only for authenticated
 * callers. The provider decides via the context - the feed service and the
 * endpoint stay free of special cases (ADR-002, ADR-005).
 */
@Component
class BngrOrderHistoryModuleProvider(
    private val purchases: BngrPurchaseRepository,
    private val simulator: BngrBackendSimulator,
) : BngrModuleProvider {

    override val order = BngrFeedSlots.ORDER_HISTORY

    override suspend fun provide(context: BngrFeedContext): BngrFeedModule? {
        val user = context.user ?: return null
        val history = simulator.simulateRemoteCall {
            purchases.findByUserEmailOrderByPurchasedAtDesc(user.email)
        }
        return BngrOrderHistoryModule(
            purchases = history.map { BngrFeedPurchase(it.productName, it.priceCents, it.purchasedAt) },
        )
    }
}
