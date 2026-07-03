package com.breuninger.homefeed.feed.api

import com.breuninger.homefeed.feed.domain.BngrFeedContext
import com.breuninger.homefeed.feed.domain.BngrFeedUser
import com.breuninger.homefeed.feed.domain.BngrHomefeedService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/** Envelope object, never a bare array: metadata can be added without breaking clients (ADR-003). */
data class BngrHomefeedResponse(
    val modules: List<BngrFeedModuleDto>,
    val meta: BngrHomefeedMeta,
)

data class BngrHomefeedMeta(
    val generatedAt: Instant,
    /** Wall-clock time of the concurrent provider fan-out — max of providers, not sum (ADR-004). */
    val assemblyTimeMs: Long,
    val moduleCount: Int,
)

@RestController
@Tag(name = "Homefeed")
class BngrHomefeedController(private val homefeedService: BngrHomefeedService) {

    @Operation(
        summary = "The ordered homefeed for the mobile app",
        description = "Authentication is optional: a valid Bearer token personalizes the greeting " +
            "and adds the protected order_history module. Anonymous callers get the public feed.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/api/v1/homefeed")
    suspend fun homefeed(authentication: Authentication?): BngrHomefeedResponse {
        val feed = homefeedService.assembleFeed(authentication.toFeedContext())
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

private fun Authentication?.toFeedContext(): BngrFeedContext {
    val jwt = (this as? JwtAuthenticationToken)?.token ?: return BngrFeedContext.ANONYMOUS
    return BngrFeedContext(
        BngrFeedUser(
            email = jwt.subject,
            firstName = jwt.getClaimAsString("firstName").orEmpty(),
            lastName = jwt.getClaimAsString("lastName").orEmpty(),
        ),
    )
}
