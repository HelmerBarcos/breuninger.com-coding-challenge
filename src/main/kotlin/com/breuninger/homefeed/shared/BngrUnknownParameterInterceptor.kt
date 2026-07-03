package com.breuninger.homefeed.shared

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

class BngrUnknownParametersException(val unknownParameters: Set<String>) :
    RuntimeException("Unknown query parameters: $unknownParameters")

/**
 * Strict query contract (ADR-010): endpoints answer only the parameters they
 * declare via @RequestParam - anything else is a 400, not silently ignored.
 * Silently ignored parameters hide client bugs (a misspelled parameter "works"
 * and just does nothing).
 */
@Component
class BngrUnknownParameterInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) return true
        val declared = handler.methodParameters
            .mapNotNull { parameter ->
                parameter.getParameterAnnotation(RequestParam::class.java)
                    ?.let { it.name.ifEmpty { parameter.parameter.name } }
            }
            .toSet()
        val unknown = request.parameterMap.keys - declared
        if (unknown.isNotEmpty()) throw BngrUnknownParametersException(unknown)
        return true
    }
}
