package com.astro.leadflow.shared.modules.monitoring.controller.dto

import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookOutcome
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookSource
import java.time.OffsetDateTime

data class WebhookEventResponse(
    val id: Long,
    val receivedAt: OffsetDateTime,
    val source: WebhookSource,
    val outcome: WebhookOutcome,
    val httpStatus: Int,
    val correlationId: String?,
    val durationMs: Int?,
    val errorMessage: String?
)
