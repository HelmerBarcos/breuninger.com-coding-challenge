package com.breuninger.homefeed.feed.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

data class BngrHomefeed(
    val modules: List<BngrFeedModule>,
    val assemblyTimeMs: Long,
)

/**
 * Assembles the homefeed by fanning out to all registered providers concurrently:
 * feed latency is the slowest provider, not the sum (ADR-004).
 *
 * Registered as a bean in BngrFeedConfiguration — this class stays framework-free.
 */
class BngrHomefeedService(providers: List<BngrModuleProvider>) {

    private val providers = providers.sortedBy { it.order }
    private val log = LoggerFactory.getLogger(BngrHomefeedService::class.java)

    suspend fun assembleFeed(context: BngrFeedContext): BngrHomefeed {
        val modules: List<BngrFeedModule?>
        val elapsedMs = measureTimeMillis {
            modules = coroutineScope {
                providers
                    .map { provider -> async { provideDegradingOnFailure(provider, context) } }
                    .awaitAll()
            }
        }
        return BngrHomefeed(modules.filterNotNull(), elapsedMs)
    }

    /**
     * A failing provider costs its own module, never the whole feed (ADR-004).
     * CancellationException must propagate — swallowing it would break structured concurrency.
     */
    private suspend fun provideDegradingOnFailure(
        provider: BngrModuleProvider,
        context: BngrFeedContext,
    ): BngrFeedModule? =
        try {
            provider.provide(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Provider {} failed, omitting its module from the feed", provider::class.simpleName, e)
            null
        }
}
