package com.breuninger.homefeed.auth.api

import com.breuninger.homefeed.auth.BngrGender
import com.breuninger.homefeed.auth.BngrRegisterUserService
import com.breuninger.homefeed.auth.BngrRegistration
import com.breuninger.homefeed.auth.BngrTokenService
import com.breuninger.homefeed.auth.BngrUserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.validation.annotation.Validated

data class BngrRegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:Size(min = 8, max = 100) val password: String,
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    val gender: BngrGender,
)

data class BngrUserDto(
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: BngrGender,
)

data class BngrLoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class BngrTokenResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val tokenType: String = "Bearer",
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
}

private fun BngrRegisterRequest.toRegistration() =
    BngrRegistration(email = email, password = password, firstName = firstName, lastName = lastName, gender = gender)

private fun BngrUserEntity.toDto() =
    BngrUserDto(email = email, firstName = firstName, lastName = lastName, gender = gender)
