package com.astro.leadflow.shared.modules.monitoring.controller.dto

import com.astro.leadflow.shared.modules.monitoring.domain.enums.LogLevel
import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationErrorResponse(
    val id: Long,
    val occurredAt: OffsetDateTime,
    val level: LogLevel,
    val loggerName: String,
    val correlationId: String?,
    val organizationId: UUID?,
    val message: String,
    val stackTrace: String?,
    val requestPath: String?,
    val requestMethod: String?,
    val userId: UUID?
)
