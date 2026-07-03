package com.breuninger.homefeed.feed.greeting

import com.breuninger.homefeed.feed.domain.BngrFeedSlots
import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedModule
import com.breuninger.homefeed.feed.domain.BngrGreetingModule
import com.breuninger.homefeed.feed.domain.BngrModuleProvider
import org.springframework.stereotype.Component

/**
 * Always present, first in the feed. Needs no backend call: it only reflects
 * the caller's identity. Anonymous callers get null names and the app renders
 * a generic greeting (ADR-008).
 */
@Component
class BngrGreetingModuleProvider : BngrModuleProvider {

    override val order = BngrFeedSlots.GREETING

    override suspend fun provide(context: BngrFeedContext): BngrFeedModule =
        BngrGreetingModule(
            firstName = context.user?.firstName,
            lastName = context.user?.lastName,
        )
}
