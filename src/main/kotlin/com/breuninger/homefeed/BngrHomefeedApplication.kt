package com.breuninger.homefeed

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BngrHomefeedApplication

fun main(args: Array<String>) {
    runApplication<BngrHomefeedApplication>(*args)
}
