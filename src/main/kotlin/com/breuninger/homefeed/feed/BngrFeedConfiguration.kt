package com.breuninger.homefeed.feed

import com.breuninger.homefeed.feed.domain.BngrHomefeedService
import com.breuninger.homefeed.feed.domain.BngrModuleProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring wiring for the framework-free feed domain: every BngrModuleProvider
 * bean in the context is discovered here — registering a provider is all a
 * domain needs to do to appear in the feed.
 */
@Configuration
class BngrFeedConfiguration {

    @Bean
    fun bngrHomefeedService(providers: List<BngrModuleProvider>) = BngrHomefeedService(providers)
}
