package com.breuninger.homefeed.shared

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

/**
 * The complete registry of machine-readable error codes this API can emit.
 * Codes are the client's contract: the app renders UI per code, never by
 * parsing messages. Messages are for developers and logs (ADR-010).
 */
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

/** One renderable error. A response carries a list of these — never loses the second problem. */
data class BngrHttpError(
    val code: BngrErrorCode,
    val message: String,
    /** The offending request field or parameter, when the error is attributable to one. */
    val field: String? = null,
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
