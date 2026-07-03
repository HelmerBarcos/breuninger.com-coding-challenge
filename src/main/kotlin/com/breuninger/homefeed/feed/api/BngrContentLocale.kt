package com.breuninger.homefeed.feed.api

import com.breuninger.homefeed.feed.domain.BngrFeedContext
import java.util.Locale

/** The content languages this service can serve; extending = add a bundle + an entry here. */
val BNGR_SUPPORTED_CONTENT_LOCALES: List<Locale> = listOf(Locale.GERMAN, Locale.ENGLISH)

/**
 * Resolves the content locale from a raw Accept-Language header (ADR-011).
 * Missing, unsupported or malformed headers degrade to the German default -
 * a wrong language header should never cost the user their feed.
 */
fun resolveContentLocale(acceptLanguage: String?): Locale {
    if (acceptLanguage.isNullOrBlank()) return BngrFeedContext.DEFAULT_LOCALE
    return try {
        Locale.lookup(Locale.LanguageRange.parse(acceptLanguage), BNGR_SUPPORTED_CONTENT_LOCALES)
            ?: BngrFeedContext.DEFAULT_LOCALE
    } catch (e: IllegalArgumentException) {
        BngrFeedContext.DEFAULT_LOCALE
    }
}
