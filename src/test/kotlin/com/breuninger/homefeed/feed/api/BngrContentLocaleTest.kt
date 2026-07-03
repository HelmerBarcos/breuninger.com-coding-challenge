package com.breuninger.homefeed.feed.api

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Locale
import kotlin.test.assertEquals

class BngrContentLocaleTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(
        nullValues = ["null"],
        value = [
            "null, de",
            "'', de",
            "de, de",
            "de-DE, de",
            "de-AT, de",
            "en, en",
            "'en-US,en;q=0.9', en",
            "'fr-FR,fr;q=0.8', de",
            "'fr;q=0.9,en;q=0.8', en",
            "'not a valid header ;;;', de",
        ],
    )
    fun `accept-language resolves to a supported content locale with german default`(header: String?, expected: String) {
        assertEquals(Locale.forLanguageTag(expected), resolveContentLocale(header))
    }
}
