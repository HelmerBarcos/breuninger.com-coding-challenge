package com.breuninger.homefeed.feed.domain

import java.util.Locale

/** Identity of the caller as far as the feed cares - mapped from the JWT at the web boundary. */
data class BngrFeedUser(
    val email: String,
    val firstName: String,
    val lastName: String,
)

data class BngrFeedContext(
    val user: BngrFeedUser?,
    /** Content locale for module texts, resolved from Accept-Language at the web boundary (ADR-011). */
    val locale: Locale = DEFAULT_LOCALE,
) {
    companion object {
        /** German shop -> German is the default content locale, not English (ADR-011). */
        val DEFAULT_LOCALE: Locale = Locale.GERMAN
        val ANONYMOUS = BngrFeedContext(user = null)
    }
}
