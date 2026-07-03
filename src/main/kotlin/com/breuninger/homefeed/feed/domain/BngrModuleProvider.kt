package com.breuninger.homefeed.feed.domain

/**
 * The extension port of the homefeed (ADR-002). Domains contribute modules by
 * implementing this interface as a Spring bean — the feed discovers them by
 * injection and never knows which domains exist.
 */
interface BngrModuleProvider {

    /** Position in the feed; lower renders first. Convention: spaced values (10, 20, 30...). */
    val order: Int

    /** Returns the module for this context, or null when the module does not apply. */
    suspend fun provide(context: BngrFeedContext): BngrFeedModule?
}
