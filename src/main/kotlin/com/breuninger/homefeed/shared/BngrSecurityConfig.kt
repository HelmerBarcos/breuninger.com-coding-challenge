package com.breuninger.homefeed.shared

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

@Configuration
class BngrSecurityConfig {

    /**
     * The feed endpoint is permitAll on purpose: authentication is optional
     * there, and the providers decide per module via the feed context — no
     * duplicated public/private routes (ADR-005). A present-but-invalid Bearer
     * token still yields 401 (resource server semantics).
     */
    @Bean
    fun bngrFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/homefeed").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/scalar", "/scalar.html").permitAll()
                    // non-health actuator endpoints stay gated
                    .requestMatchers("/actuator/**").authenticated()
                    // everything else falls through to MVC so unknown paths answer a 404
                    // problem body (ADR-010) instead of a misleading 401 from the filter
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer { it.jwt {} }
            .build()

    @Bean
    fun bngrPasswordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * In-memory RSA keypair, regenerated on every start: issued tokens die with
     * the process — an accepted trade-off at this stage (ADR-005). A real
     * deployment would point the resource server at an external issuer/JWKS.
     */
    @Bean
    fun bngrRsaKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    @Bean
    fun bngrJwtEncoder(keyPair: KeyPair): JwtEncoder {
        val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID("bngr-homefeed-dev")
            .build()
        return NimbusJwtEncoder(ImmutableJWKSet<SecurityContext>(JWKSet(jwk)))
    }

    @Bean
    fun bngrJwtDecoder(keyPair: KeyPair): JwtDecoder =
        NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()
}
