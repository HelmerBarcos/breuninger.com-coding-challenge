package com.breuninger.homefeed.shared

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class BngrWebConfig : WebMvcConfigurer {

    /** /scalar serves the Scalar API reference (static/scalar.html renders /v3/api-docs). */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/scalar", "/scalar.html")
    }

    @Bean
    fun bngrOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Breuninger Homefeed API")
                .description(
                    "Composes the mobile app homefeed as an ordered list of typed modules. " +
                        "Authentication is optional on the feed endpoint; a Bearer token adds protected modules.",
                )
                .version("v1"),
        )
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ),
        )
}
