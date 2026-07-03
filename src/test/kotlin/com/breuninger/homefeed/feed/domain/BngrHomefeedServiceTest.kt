package com.breuninger.homefeed.feed.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BngrHomefeedServiceTest {

    private fun provider(order: Int, module: BngrFeedModule?, delayMs: Long = 0) =
        object : BngrModuleProvider {
            override val order = order
            override suspend fun provide(context: BngrFeedContext): BngrFeedModule? {
                delay(delayMs)
                return module
            }
        }

    private val greeting = BngrGreetingModule(firstName = null, lastName = null)
    private val banner = BngrSaleBannerModule(headline = "Sale", ctaLabel = "Shop", imageUrl = "img")
    private val teaser = BngrProductTeaserModule(headline = "New", products = emptyList())

    @Test
    fun `modules come back sorted by provider order, not by completion order`() = runTest {
        // the first-positioned provider is the slowest — order must still win
        val service = BngrHomefeedService(
            listOf(
                provider(order = 30, module = teaser),
                provider(order = 10, module = greeting, delayMs = 500),
                provider(order = 20, module = banner, delayMs = 100),
            ),
        )

        val feed = service.assembleFeed(BngrFeedContext.ANONYMOUS)

        assertEquals(listOf(greeting, banner, teaser), feed.modules)
    }

    @Test
    fun `a provider returning null is omitted without leaving a gap`() = runTest {
        val service = BngrHomefeedService(
            listOf(
                provider(order = 10, module = greeting),
                provider(order = 20, module = null),
                provider(order = 30, module = teaser),
            ),
        )

        val feed = service.assembleFeed(BngrFeedContext.ANONYMOUS)

        assertEquals(listOf(greeting, teaser), feed.modules)
    }

    @Test
    fun `a throwing provider degrades to an omitted module instead of failing the feed`() = runTest {
        val failing = object : BngrModuleProvider {
            override val order = 20
            override suspend fun provide(context: BngrFeedContext): BngrFeedModule =
                error("campaign backend is down")
        }
        val service = BngrHomefeedService(
            listOf(provider(order = 10, module = greeting), failing, provider(order = 30, module = teaser)),
        )

        val feed = service.assembleFeed(BngrFeedContext.ANONYMOUS)

        assertEquals(listOf(greeting, teaser), feed.modules)
    }

    @Test
    fun `an empty provider list yields an empty feed`() = runTest {
        val feed = BngrHomefeedService(emptyList()).assembleFeed(BngrFeedContext.ANONYMOUS)

        assertEquals(emptyList(), feed.modules)
    }
}
