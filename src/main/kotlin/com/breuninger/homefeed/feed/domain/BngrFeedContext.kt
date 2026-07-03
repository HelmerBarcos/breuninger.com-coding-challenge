package com.breuninger.homefeed.feed.domain

/** Identity of the caller as far as the feed cares - mapped from the JWT at the web boundary. */
data class BngrFeedUser(
    val email: String,
    val firstName: String,
    val lastName: String,
)

data class BngrFeedContext(val user: BngrFeedUser?) {
    companion object {
        val ANONYMOUS = BngrFeedContext(user = null)
    }
}
