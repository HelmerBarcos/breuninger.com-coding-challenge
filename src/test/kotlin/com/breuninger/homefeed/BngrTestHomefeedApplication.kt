package com.breuninger.homefeed

import org.springframework.boot.fromApplication
import org.springframework.boot.with

/** Local run with a throwaway Testcontainers Postgres: ./mvnw spring-boot:test-run */
fun main(args: Array<String>) {
    fromApplication<BngrHomefeedApplication>().with(BngrTestcontainersConfiguration::class).run(*args)
}
