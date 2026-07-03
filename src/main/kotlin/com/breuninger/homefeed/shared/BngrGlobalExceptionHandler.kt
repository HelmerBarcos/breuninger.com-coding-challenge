package com.breuninger.homefeed.shared

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Cross-cutting error contract (ADR-010): every response is an RFC 9457
 * problem carrying an `errors[]` array of BngrHttpError, so a single response
 * can report several problems at once. Domain-specific errors (auth) live
 * next to their controllers and use the same shape.
 */
@RestControllerAdvice
class BngrGlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(BngrGlobalExceptionHandler::class.java)

    /** All validation failures at once - the client fixes the form in one round trip. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidBody(e: MethodArgumentNotValidException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            e.bindingResult.fieldErrors.map {
                BngrHttpError(BngrErrorCode.FIELD_INVALID, it.defaultMessage ?: "invalid value", field = it.field)
            },
        )

    @ExceptionHandler(BngrUnknownParametersException::class)
    fun unknownParameters(e: BngrUnknownParametersException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.BAD_REQUEST,
            "Unknown query parameters",
            e.unknownParameters.map {
                BngrHttpError(BngrErrorCode.UNKNOWN_PARAMETER, "parameter is not supported by this endpoint", field = it)
            },
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadableBody(e: HttpMessageNotReadableException): ProblemDetail =
        when (val cause = e.rootCause) {
            is UnrecognizedPropertyException -> bngrProblemOf(
                HttpStatus.BAD_REQUEST,
                "Request body contains unknown fields",
                BngrHttpError(BngrErrorCode.UNKNOWN_FIELD, "field is not part of this request", field = cause.propertyName),
            )
            is InvalidFormatException -> bngrProblemOf(
                HttpStatus.BAD_REQUEST,
                "Request body has a field with an invalid value",
                BngrHttpError(
                    BngrErrorCode.FIELD_INVALID,
                    "invalid value, expected ${cause.targetType.simpleName}",
                    field = cause.path.joinToString(".") { it.fieldName ?: "[]" },
                ),
            )
            else -> bngrProblemOf(
                HttpStatus.BAD_REQUEST,
                "Request body could not be parsed",
                BngrHttpError(BngrErrorCode.MALFORMED_BODY, "body is not valid JSON for this endpoint"),
            )
        }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun missingParameter(e: MissingServletRequestParameterException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.BAD_REQUEST,
            "Required query parameter is missing",
            BngrHttpError(BngrErrorCode.MISSING_PARAMETER, "parameter is required", field = e.parameterName),
        )

    /** Unknown paths answer 404 with the same JSON shape as every other error (ADR-010). */
    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun unknownPath(): ProblemDetail =
        bngrProblemOf(
            HttpStatus.NOT_FOUND,
            "The requested resource does not exist",
            BngrHttpError(BngrErrorCode.NOT_FOUND, "unknown endpoint"),
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun methodNotAllowed(e: HttpRequestMethodNotSupportedException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method ${e.method} is not supported by this endpoint",
            BngrHttpError(BngrErrorCode.METHOD_NOT_ALLOWED, "use one of: ${e.supportedHttpMethods?.joinToString() ?: "-"}"),
        )

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun unsupportedMediaType(e: HttpMediaTypeNotSupportedException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Content type is not supported",
            BngrHttpError(BngrErrorCode.UNSUPPORTED_MEDIA_TYPE, "send application/json"),
        )

    /** Last resort: never leak internals to the client; the stack trace goes to the log. */
    @ExceptionHandler(Exception::class)
    fun unexpected(e: Exception): ProblemDetail {
        log.error("Unhandled exception", e)
        return bngrProblemOf(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            BngrHttpError(BngrErrorCode.INTERNAL_ERROR, "please retry; contact support if it persists"),
        )
    }
}
