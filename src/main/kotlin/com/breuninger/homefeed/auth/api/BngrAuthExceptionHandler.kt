package com.breuninger.homefeed.auth.api

import com.breuninger.homefeed.auth.BngrEmailTakenException
import com.breuninger.homefeed.auth.BngrInvalidCredentialsException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** RFC 9457 problem responses for the auth flows (basic on purpose — see README). */
@RestControllerAdvice
class BngrAuthExceptionHandler {

    @ExceptionHandler(BngrEmailTakenException::class)
    fun emailTaken(e: BngrEmailTakenException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Email already registered")

    @ExceptionHandler(BngrInvalidCredentialsException::class)
    fun invalidCredentials(e: BngrInvalidCredentialsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.message ?: "Invalid credentials")
}
