package com.breuninger.homefeed.auth.api

import com.breuninger.homefeed.auth.BngrEmailTakenException
import com.breuninger.homefeed.auth.BngrInvalidCredentialsException
import com.breuninger.homefeed.shared.BngrErrorCode
import com.breuninger.homefeed.shared.BngrHttpError
import com.breuninger.homefeed.shared.bngrProblemOf
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Auth-domain errors in the shared error shape (ADR-010). */
@RestControllerAdvice
class BngrAuthExceptionHandler {

    @ExceptionHandler(BngrEmailTakenException::class)
    fun emailTaken(e: BngrEmailTakenException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.CONFLICT,
            "Registration rejected",
            BngrHttpError(BngrErrorCode.EMAIL_TAKEN, e.message ?: "email already registered", field = "email"),
        )

    @ExceptionHandler(BngrInvalidCredentialsException::class)
    fun invalidCredentials(e: BngrInvalidCredentialsException): ProblemDetail =
        bngrProblemOf(
            HttpStatus.UNAUTHORIZED,
            "Login rejected",
            // deliberately does not say whether email or password was wrong
            BngrHttpError(BngrErrorCode.INVALID_CREDENTIALS, "invalid email or password"),
        )
}
