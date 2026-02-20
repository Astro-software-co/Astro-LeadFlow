package com.astro.leadflow

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AstroApplication

fun main(args: Array<String>) {
    runApplication<AstroApplication>(*args)
}
