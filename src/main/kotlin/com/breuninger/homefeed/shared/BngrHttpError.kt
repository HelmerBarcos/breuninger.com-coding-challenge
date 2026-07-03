package com.breuninger.homefeed.shared

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

/**
 * The complete registry of machine-readable error codes this API can emit.
 * Codes are the client's contract: the app renders UI per code, never by
 * parsing messages. Messages are for developers and logs (ADR-010).
 */
@Schema(description = "Machine-readable error code - the client renders error UI per code, never by parsing messages. This enum is the complete registry of codes this API can emit.")
enum class BngrErrorCode {
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    FIELD_INVALID,
    UNKNOWN_FIELD,
    UNKNOWN_PARAMETER,
    MISSING_PARAMETER,
    MALFORMED_BODY,
    EMAIL_TAKEN,
    INVALID_CREDENTIALS,
    INTERNAL_ERROR,
}

/** One renderable error. A response carries a list of these - never loses the second problem. */
@Schema(description = "One renderable error. Error responses carry a list of these, so a single response can report several problems at once (e.g. two invalid form fields).")
data class BngrHttpError(
    val code: BngrErrorCode,
    @get:Schema(description = "Developer-facing explanation - may change without notice; render UI from `code`.", example = "must be a well-formed email address")
    val message: String,
    /** The offending request field or parameter, when the error is attributable to one. */
    @get:Schema(description = "The offending request field or query parameter, when attributable.", example = "email")
    val field: String? = null,
)

/**
 * Documentation-only mirror of the wire format (a ProblemDetail extended with
 * `errors` - ADR-010): referenced from @ApiResponse annotations so the error
 * shape and the code registry show up in the interactive API reference.
 */
@Schema(name = "BngrProblem", description = "RFC 9457 problem (`application/problem+json`) extended with the `errors[]` array (ADR-010). Sent for every non-2xx response.")
data class BngrProblemResponse(
    @get:Schema(description = "HTTP status code, repeated in the body.", example = "400")
    val status: Int,
    @get:Schema(description = "Brief, human-readable server explanation of what went wrong.", example = "Request validation failed")
    val detail: String,
    @get:Schema(description = "The individual errors - at least one, possibly several.")
    val errors: List<BngrHttpError>,
)

/**
 * RFC 9457 problem extended with the `errors` array (ADR-010): standard
 * `status` + `detail` for humans and infrastructure, `errors[].code` for the
 * client UI.
 */
fun bngrProblemOf(status: HttpStatus, detail: String, errors: List<BngrHttpError>): ProblemDetail =
    ProblemDetail.forStatusAndDetail(status, detail).apply {
        setProperty("errors", errors)
    }

fun bngrProblemOf(status: HttpStatus, detail: String, error: BngrHttpError): ProblemDetail =
    bngrProblemOf(status, detail, listOf(error))
