package com.astro.leadflow.shared.modules.monitoring.error

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class DatabaseErrorAppenderRegistrar(
    private val writer: ApplicationErrorWriter
) {
    @PostConstruct
    fun register() {
        DatabaseErrorAppenderRegistry.writer = writer
    }
}
