package com.breuninger.homefeed.feed.api

import com.breuninger.homefeed.shared.BngrJwtClaims
import com.breuninger.homefeed.shared.BngrApiPaths
import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedUser
import com.breuninger.homefeed.feed.domain.BngrHomefeedService
import com.breuninger.homefeed.shared.BngrProblemResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/** Envelope object, never a bare array: metadata can be added without breaking clients (ADR-003). */
@Schema(description = "The homefeed envelope. `modules` is the ordered list the app renders top to bottom; `meta` is diagnostic metadata.")
data class BngrHomefeedResponse(
    @get:Schema(description = "Feed modules in render order. Clients must skip unknown `type` values.")
    val modules: List<BngrFeedModuleDto>,
    val meta: BngrHomefeedMeta,
)

@Schema(description = "Diagnostic metadata about how this feed was assembled.")
data class BngrHomefeedMeta(
    @get:Schema(description = "Server time at which the feed was assembled.")
    val generatedAt: Instant,
    /** Wall-clock time of the concurrent provider fan-out - max of providers, not sum (ADR-004). */
    @get:Schema(description = "Wall-clock milliseconds of the concurrent module fan-out - roughly the slowest provider, not the sum.", example = "163")
    val assemblyTimeMs: Long,
    @get:Schema(description = "Number of modules in this response.", example = "5")
    val moduleCount: Int,
)

@RestController
@Tag(name = "Homefeed")
class BngrHomefeedController(private val homefeedService: BngrHomefeedService) {

    @Operation(
        summary = "The ordered homefeed for the mobile app",
        description = "Authentication is **optional**: call it without any Authorization header for the " +
            "public feed, or authenticate (padlock above) to personalize the greeting and receive the " +
            "protected order_history module. Note: a *present but invalid* Bearer token is rejected " +
            "with 401 - omit the header entirely for the anonymous feed.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "The ordered feed. Module set depends on authentication.",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = BngrHomefeedResponse::class),
                examples = [
                    ExampleObject(
                        name = "anonymous",
                        summary = "Without Authorization header - 4 public modules, greeting names are null",
                        value = """{
                          "modules": [
                            { "type": "greeting", "firstName": null, "lastName": null },
                            { "type": "sale_banner", "headline": "Mid-Season Sale - bis zu 30%", "ctaLabel": "Jetzt shoppen", "imageUrl": "https://placehold.co/1200x400" },
                            { "type": "product_teaser", "headline": "Neu bei Breuninger", "products": [ { "id": "11111111-0000-0000-0000-000000000001", "name": "Cashmere Crewneck Sweater", "brand": "Breuninger Collection", "priceCents": 19900, "imageUrl": "https://placehold.co/600x800" } ] },
                            { "type": "recommendations", "headline": "Für dich empfohlen", "products": [ { "id": "11111111-0000-0000-0000-000000000005", "name": "Silk Twill Scarf", "brand": "Gucci", "priceCents": 21900, "imageUrl": "https://placehold.co/600x800" } ] }
                          ],
                          "meta": { "generatedAt": "2026-07-03T12:00:00Z", "assemblyTimeMs": 163, "moduleCount": 4 }
                        }""",
                    ),
                    ExampleObject(
                        name = "authenticated",
                        summary = "With a valid Bearer token - personalized greeting plus order_history",
                        value = """{
                          "modules": [
                            { "type": "greeting", "firstName": "Felix", "lastName": "Junghans" },
                            { "type": "sale_banner", "headline": "Mid-Season Sale - bis zu 30%", "ctaLabel": "Jetzt shoppen", "imageUrl": "https://placehold.co/1200x400" },
                            { "type": "product_teaser", "headline": "Neu bei Breuninger", "products": [ { "id": "11111111-0000-0000-0000-000000000001", "name": "Cashmere Crewneck Sweater", "brand": "Breuninger Collection", "priceCents": 19900, "imageUrl": "https://placehold.co/600x800" } ] },
                            { "type": "recommendations", "headline": "Für dich empfohlen", "products": [ { "id": "11111111-0000-0000-0000-000000000005", "name": "Silk Twill Scarf", "brand": "Gucci", "priceCents": 21900, "imageUrl": "https://placehold.co/600x800" } ] },
                            { "type": "order_history", "purchases": [ { "productName": "Leather Chelsea Boots", "priceCents": 34900, "purchasedAt": "2026-06-26T09:30:00Z" } ] }
                          ],
                          "meta": { "generatedAt": "2026-07-03T12:00:00Z", "assemblyTimeMs": 171, "moduleCount": 5 }
                        }""",
                    ),
                ],
            ),
        ],
    )
    @ApiResponse(
        responseCode = "400",
        description = "Unknown query parameters - this endpoint declares none, anything sent is rejected (code UNKNOWN_PARAMETER, one error per parameter).",
        content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = BngrProblemResponse::class))],
    )
    @ApiResponse(
        responseCode = "401",
        description = "An Authorization header was sent but the token is invalid or expired. Omit the header entirely for the anonymous feed.",
        content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = BngrProblemResponse::class))],
    )
    @GetMapping(BngrApiPaths.HOMEFEED)
    suspend fun homefeed(
        authentication: Authentication?,
        @Parameter(description = "Content language: `de` (default) or `en`. Only module content is localized - error messages stay English (ADR-010/011).")
        @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false)
        acceptLanguage: String?,
    ): BngrHomefeedResponse {
        val context = BngrFeedContext(
            user = authentication.toFeedUser(),
            locale = resolveContentLocale(acceptLanguage),
        )
        val feed = homefeedService.assembleFeed(context)
        return BngrHomefeedResponse(
            modules = feed.modules.map { it.toDto() },
            meta = BngrHomefeedMeta(
                generatedAt = Instant.now(),
                assemblyTimeMs = feed.assemblyTimeMs,
                moduleCount = feed.modules.size,
            ),
        )
    }
}

private fun Authentication?.toFeedUser(): BngrFeedUser? {
    val jwt = (this as? JwtAuthenticationToken)?.token ?: return null
    return BngrFeedUser(
        email = jwt.subject,
        firstName = jwt.getClaimAsString(BngrJwtClaims.FIRST_NAME).orEmpty(),
        lastName = jwt.getClaimAsString(BngrJwtClaims.LAST_NAME).orEmpty(),
    )
}
