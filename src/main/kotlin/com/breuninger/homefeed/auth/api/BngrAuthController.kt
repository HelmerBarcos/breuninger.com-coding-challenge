package com.breuninger.homefeed.auth.api

import com.breuninger.homefeed.auth.BngrGender
import com.breuninger.homefeed.auth.BngrRegisterUserService
import com.breuninger.homefeed.auth.BngrRegistration
import com.breuninger.homefeed.auth.BngrTokenService
import com.breuninger.homefeed.auth.BngrUserEntity
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.validation.annotation.Validated

@Schema(description = "New account data. Registering automatically seeds 3-5 mock purchases for the user — there are no purchase endpoints.")
data class BngrRegisterRequest(
    @field:Email @field:NotBlank
    @get:Schema(example = "anna.muster@example.com")
    val email: String,
    @field:Size(min = 8, max = 100)
    @get:Schema(description = "8-100 characters.", example = "super-secret-1")
    val password: String,
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @get:Schema(description = "One of the three legal genders in Germany (männlich / weiblich / divers).")
    val gender: BngrGender,
)

@Schema(description = "Public representation of a user account — never contains credentials.")
data class BngrUserDto(
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: BngrGender,
)

@Schema(description = "Credentials to exchange for a Bearer token.")
data class BngrLoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

@Schema(description = "Short-lived JWT for the Authorization header: `Authorization: Bearer <accessToken>`.")
data class BngrTokenResponse(
    val accessToken: String,
    @get:Schema(description = "Token lifetime in seconds.", example = "3600")
    val expiresInSeconds: Long,
    @get:Schema(example = "Bearer")
    val tokenType: String = "Bearer",
)

@Schema(description = "OAuth2-shaped token response (snake_case fields) so interactive API docs can use the password flow.")
data class BngrOAuthTokenResponse(
    @get:JsonProperty("access_token") val accessToken: String,
    @get:JsonProperty("expires_in") val expiresIn: Long,
    @get:JsonProperty("token_type") val tokenType: String = "Bearer",
)

@RestController
@Validated
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
class BngrAuthController(
    private val registerUserService: BngrRegisterUserService,
    private val tokenService: BngrTokenService,
) {

    @Operation(summary = "Register a new user (mock purchases are seeded automatically)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody @Valid request: BngrRegisterRequest): BngrUserDto =
        registerUserService.register(request.toRegistration()).toDto()

    @Operation(summary = "Exchange credentials for a Bearer token")
    @PostMapping("/login")
    fun login(@RequestBody @Valid request: BngrLoginRequest): BngrTokenResponse {
        val issued = tokenService.login(request.email, request.password)
        return BngrTokenResponse(accessToken = issued.accessToken, expiresInSeconds = issued.expiresInSeconds)
    }

    /**
     * Same login, OAuth2 password-grant shape (form-encoded in, snake_case out):
     * interactive API references (Scalar) authenticate against this live instead
     * of asking users to paste tokens.
     */
    @Operation(summary = "OAuth2-style token endpoint (used by the interactive API reference to fetch tokens live)")
    @PostMapping("/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun token(
        @RequestParam username: String,
        @RequestParam password: String,
    ): BngrOAuthTokenResponse {
        val issued = tokenService.login(username, password)
        return BngrOAuthTokenResponse(accessToken = issued.accessToken, expiresIn = issued.expiresInSeconds)
    }
}

private fun BngrRegisterRequest.toRegistration() =
    BngrRegistration(email = email, password = password, firstName = firstName, lastName = lastName, gender = gender)

private fun BngrUserEntity.toDto() =
    BngrUserDto(email = email, firstName = firstName, lastName = lastName, gender = gender)
