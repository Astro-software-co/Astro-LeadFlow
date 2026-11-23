package com.astro.leadflow.shared.modules.monitoring.webhook

import com.astro.leadflow.shared.modules.monitoring.domain.entity.WebhookEventLog
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookOutcome
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookSource
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class WebhookEventLoggerTest {
    @Test
    fun `log saves event and increments metric`() {
        val repository = mockk<WebhookEventLogRepository>()
        val registry = SimpleMeterRegistry()
        every { repository.save(any<WebhookEventLog>()) } answers { firstArg() }

        WebhookEventLogger(repository, registry).log(WebhookSource.META, WebhookOutcome.SUCCESS, 200, 12, "corr")

        verify { repository.save(match { it.source == WebhookSource.META && it.durationMs == 12 }) }
    }
}
