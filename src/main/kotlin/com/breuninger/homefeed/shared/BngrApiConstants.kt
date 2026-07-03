package com.breuninger.homefeed.shared

/** Every path the service exposes — security rules, controllers and docs reference these, never literals. */
object BngrApiPaths {
    const val API_V1 = "/api/v1"

    const val HOMEFEED = "$API_V1/homefeed"

    const val AUTH = "$API_V1/auth"
    const val AUTH_REGISTER = "$AUTH/register"
    const val AUTH_LOGIN = "$AUTH/login"
    const val AUTH_TOKEN = "$AUTH/token"

    const val OPENAPI_SPEC = "/v3/api-docs"
    const val SCALAR = "/scalar"
    const val SCALAR_HTML = "/scalar.html"
    const val HEALTH = "/actuator/health"
}

/** Custom claims carried in the self-issued JWT (ADR-005) — written by the issuer, read at the web boundary. */
object BngrJwtClaims {
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val GENDER = "gender"
}

/** OpenAPI security scheme names (referenced by the spec customizers). */
object BngrSecuritySchemes {
    const val BEARER = "bearerAuth"
    const val OAUTH_PASSWORD = "oauthPassword"
}
