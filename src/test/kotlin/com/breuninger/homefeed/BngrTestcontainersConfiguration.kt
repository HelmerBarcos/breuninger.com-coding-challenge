package com.breuninger.homefeed

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class BngrTestcontainersConfiguration {

    /** Same image as docker-compose — tests run against the database we actually ship with. */
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
}
