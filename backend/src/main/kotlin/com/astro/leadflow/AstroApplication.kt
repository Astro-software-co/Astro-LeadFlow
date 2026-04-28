package com.astro.leadflow

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AstroApplication

fun main(args: Array<String>) {
    runApplication<AstroApplication>(*args)
}
