package com.astro.leadflow.shared.modules.monitoring.error

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import com.astro.leadflow.shared.modules.monitoring.domain.entity.ApplicationError
import com.astro.leadflow.shared.modules.monitoring.domain.enums.LogLevel
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ApplicationErrorWriter(
    private val repository: ApplicationErrorRepository
) {
    fun write(event: ILoggingEvent) {
        val level = when (event.level.levelStr) {
            "ERROR" -> LogLevel.ERROR
            "WARN" -> LogLevel.WARN
            else -> return
        }
        val mdc = event.mdcPropertyMap.orEmpty()
        repository.save(
            ApplicationError(
                level = level,
                loggerName = event.loggerName,
                correlationId = mdc["correlationId"] ?: mdc["correlation-id"],
                organizationId = parseUuid(mdc["orgId"] ?: mdc["organizationId"]),
                message = event.formattedMessage.take(4000),
                stackTrace = event.throwableProxy?.toStackTraceString(),
                requestPath = mdc["requestPath"],
                requestMethod = mdc["requestMethod"],
                userId = parseUuid(mdc["userId"])
            )
        )
    }

    private fun parseUuid(value: String?): UUID? =
        runCatching { value?.let(UUID::fromString) }.getOrNull()

    private fun IThrowableProxy.toStackTraceString(): String =
        buildString {
            append(className).append(": ").append(message).append('\n')
            stackTraceElementProxyArray?.forEach { append("\tat ").append(it.steAsString).append('\n') }
        }
}
