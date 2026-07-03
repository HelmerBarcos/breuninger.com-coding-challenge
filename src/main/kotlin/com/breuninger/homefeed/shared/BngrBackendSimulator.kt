package com.breuninger.homefeed.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@ConfigurationProperties("homefeed.simulation")
data class BngrSimulationProperties(
    /** Fake latency each provider pays per "remote call". Set to 0 in tests. */
    val latency: Duration = Duration.ofMillis(150),
)

/**
 * Stands in for the network hop to another backend (catalog service, order
 * service, ...). Providers run their repository access through it so the
 * concurrent fan-out (ADR-004) has something real to parallelize, and blocking
 * JPA calls stay off the request coroutine's thread.
 */
@Component
class BngrBackendSimulator(private val properties: BngrSimulationProperties) {

    suspend fun <T> simulateRemoteCall(block: () -> T): T {
        delay(properties.latency.toMillis())
        return withContext(Dispatchers.IO) { block() }
    }
}
