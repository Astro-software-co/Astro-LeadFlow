package com.astro.leadflow.shared.modules.monitoring.webhook

import com.astro.leadflow.shared.modules.monitoring.domain.entity.WebhookEventLog
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookOutcome
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookSource
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

@Service
class WebhookEventLogger(
    private val repository: WebhookEventLogRepository,
    private val meterRegistry: MeterRegistry
) {
    fun log(
        source: WebhookSource,
        outcome: WebhookOutcome,
        httpStatus: Int,
        durationMs: Int? = null,
        correlationId: String? = null,
        errorMessage: String? = null
    ) {
        repository.save(
            WebhookEventLog(
                source = source,
                outcome = outcome,
                httpStatus = httpStatus,
                durationMs = durationMs,
                correlationId = correlationId,
                errorMessage = errorMessage
            )
        )
        meterRegistry.counter("webhook.received", "source", source.name, "outcome", outcome.name).increment()
    }
}
