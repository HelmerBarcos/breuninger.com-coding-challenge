package com.breuninger.homefeed.feed.domain

/**
 * The extension port of the homefeed (ADR-002). Domains contribute modules by
 * implementing this interface as a Spring bean - the feed discovers them by
 * injection and never knows which domains exist.
 */
interface BngrModuleProvider {

    /** Position in the feed; lower renders first. Take slots from [BngrFeedSlots]. */
    val order: Int

    /** Returns the module for this context, or null when the module does not apply. */
    suspend fun provide(context: BngrFeedContext): BngrFeedModule?
}

/**
 * The feed's slot allocation, spaced so new modules fit in between. One place
 * to look means order collisions are visible in a single diff.
 */
object BngrFeedSlots {
    const val GREETING = 10
    const val SALE_BANNER = 20
    const val PRODUCT_TEASER = 30
    const val RECOMMENDATIONS = 40
    const val ORDER_HISTORY = 50
}
