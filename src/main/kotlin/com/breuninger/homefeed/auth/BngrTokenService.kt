package com.breuninger.homefeed.auth

import com.breuninger.homefeed.shared.BngrJwtClaims
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

class BngrInvalidCredentialsException : RuntimeException("Invalid email or password")

data class BngrIssuedToken(val accessToken: String, val expiresInSeconds: Long)

/**
 * Issues the JWTs this service itself validates as an OAuth2 resource server
 * (ADR-005). Swapping to a real IdP later means deleting this issuer and
 * pointing the resource server at an external JWKS.
 */
@Service
class BngrTokenService(
    private val users: BngrUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
) {

    fun login(email: String, password: String): BngrIssuedToken {
        val user = users.findByEmail(email) ?: throw BngrInvalidCredentialsException()
        if (!passwordEncoder.matches(password, user.passwordHash)) throw BngrInvalidCredentialsException()

        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(user.email)
            .issuedAt(now)
            .expiresAt(now.plus(TOKEN_TTL))
            .claim(BngrJwtClaims.FIRST_NAME, user.firstName)
            .claim(BngrJwtClaims.LAST_NAME, user.lastName)
            .claim(BngrJwtClaims.GENDER, user.gender.name)
            .build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(claims))
        return BngrIssuedToken(accessToken = token.tokenValue, expiresInSeconds = TOKEN_TTL.seconds)
    }

    companion object {
        const val ISSUER = "bngr-homefeed"
        val TOKEN_TTL: Duration = Duration.ofHours(1)
    }
}
