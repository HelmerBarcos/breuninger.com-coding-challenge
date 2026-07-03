package com.breuninger.homefeed.shared

import org.slf4j.LoggerFactory
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * Logs where the interactive API reference lives once the server has a real
 * port (works with a fixed port, a random test port, or inside the container).
 */
@Component
class BngrApiReferenceLogger : ApplicationListener<WebServerInitializedEvent> {

    private val log = LoggerFactory.getLogger(BngrApiReferenceLogger::class.java)

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        log.info("Scalar API reference ready: http://localhost:{}/scalar (OpenAPI spec: http://localhost:{}/v3/api-docs)", port, port)
    }
}
