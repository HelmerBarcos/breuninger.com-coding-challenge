package com.breuninger.homefeed.shared

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class BngrWebConfig(
    private val unknownParameterInterceptor: BngrUnknownParameterInterceptor,
) : WebMvcConfigurer {

    /** /scalar serves the Scalar API reference (static/scalar.html renders /v3/api-docs). */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController(BngrApiPaths.SCALAR, BngrApiPaths.SCALAR_HTML)
    }

    /** Strictness is scoped to our API — actuator and docs keep their own conventions. */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(unknownParameterInterceptor).addPathPatterns("${BngrApiPaths.API_V1}/**")
    }

    @Bean
    fun bngrOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Breuninger Homefeed API")
                .description(
                    "Composes the mobile app homefeed as an ordered list of typed modules. " +
                        "Authentication is optional on the feed endpoint; a Bearer token adds protected modules. " +
                        "Authenticate below with a demo user (e.g. felix.junghans@breuninger.de / breuninger-demo) " +
                        "to try the personalized feed live.",
                )
                .version("v1"),
        )
        .components(
            Components()
                .addSecuritySchemes(
                    BngrSecuritySchemes.BEARER,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Paste a token obtained from ${BngrApiPaths.AUTH_LOGIN}."),
                )
                .addSecuritySchemes(
                    BngrSecuritySchemes.OAUTH_PASSWORD,
                    SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("Live login for the interactive reference: username = email of a demo user, e.g. felix.junghans@breuninger.de / breuninger-demo.")
                        .flows(
                            OAuthFlows().password(
                                OAuthFlow().tokenUrl(BngrApiPaths.AUTH_TOKEN).scopes(Scopes()),
                            ),
                        ),
                ),
        )

    /**
     * OpenAPI expresses "authentication is optional" as a security list that
     * contains an EMPTY requirement alongside the real ones — not expressible
     * with the @Operation annotation alone. Without this, interactive clients
     * (Scalar) always attach an Authorization header, and a present-but-empty
     * Bearer token is correctly rejected with 401.
     */
    @Bean
    fun bngrOptionalFeedAuthCustomizer(): OpenApiCustomizer = OpenApiCustomizer { api ->
        api.paths[BngrApiPaths.HOMEFEED]?.get?.security = listOf(
            SecurityRequirement(), // anonymous is a first-class way to call this endpoint
            SecurityRequirement().addList(BngrSecuritySchemes.BEARER),
            SecurityRequirement().addList(BngrSecuritySchemes.OAUTH_PASSWORD),
        )
    }
}
