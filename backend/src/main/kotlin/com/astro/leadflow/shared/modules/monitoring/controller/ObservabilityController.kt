package com.astro.leadflow.shared.modules.monitoring.controller

import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.monitoring.controller.dto.ApplicationErrorResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.ChatbotInsightsResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.ObservabilityOverviewResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.PageResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.PerformanceResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.WebhookEventResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.WebhookSummaryItemResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.WebhookSummaryResponse
import com.astro.leadflow.shared.modules.monitoring.domain.entity.ApplicationError
import com.astro.leadflow.shared.modules.monitoring.domain.entity.WebhookEventLog
import com.astro.leadflow.shared.modules.monitoring.domain.enums.LogLevel
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookOutcome
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookSource
import com.astro.leadflow.shared.modules.monitoring.error.ApplicationErrorRepository
import com.astro.leadflow.shared.modules.monitoring.service.ChatbotInsightsService
import com.astro.leadflow.shared.modules.monitoring.service.HealthAggregatorService
import com.astro.leadflow.shared.modules.monitoring.service.PerformanceMetricsService
import com.astro.leadflow.shared.modules.monitoring.webhook.WebhookEventLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/admin/observability")
@PreAuthorize("hasRole('ASTRO_ADMIN')")
class ObservabilityController(
    private val healthAggregatorService: HealthAggregatorService,
    private val performanceMetricsService: PerformanceMetricsService,
    private val applicationErrorRepository: ApplicationErrorRepository,
    private val webhookEventLogRepository: WebhookEventLogRepository,
    private val chatbotInsightsService: ChatbotInsightsService
) {
    @GetMapping("/overview")
    fun overview(): ObservabilityOverviewResponse = healthAggregatorService.overview()

    @GetMapping("/performance")
    fun performance(): PerformanceResponse = performanceMetricsService.performance()

    @GetMapping("/errors")
    fun errors(
        @RequestParam(required = false) level: LogLevel?,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(required = false) service: String?,
        @RequestParam(required = false) correlationId: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<ApplicationErrorResponse> =
        applicationErrorRepository.search(level, from, to, service, correlationId, PageRequest.of(page, size.coerceIn(1, 100)))
            .toPageResponse { it.toResponse() }

    @GetMapping("/errors/{id}")
    fun error(@PathVariable id: Long): ApplicationErrorResponse =
        applicationErrorRepository.findById(id).orElseThrow { ResourceNotFoundException("ApplicationError", id) }.toResponse()

    @GetMapping("/webhooks")
    fun webhooks(
        @RequestParam(required = false) source: WebhookSource?,
        @RequestParam(required = false) outcome: WebhookOutcome?,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<WebhookEventResponse> =
        webhookEventLogRepository.search(source, outcome, from, to, PageRequest.of(page, size.coerceIn(1, 100)))
            .toPageResponse { it.toResponse() }

    @GetMapping("/webhooks/summary")
    fun webhookSummary(
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?
    ): WebhookSummaryResponse =
        WebhookSummaryResponse(
            webhookEventLogRepository.summarize(from, to).map {
                WebhookSummaryItemResponse(it[0].toString(), it[1].toString(), (it[2] as Number).toLong())
            }
        )

    @GetMapping("/chatbot-insights")
    fun chatbotInsights(): ChatbotInsightsResponse = chatbotInsightsService.insights()

    @GetMapping("/infrastructure")
    fun infrastructure(): Map<String, Any?> = healthAggregatorService.infrastructure()

    private fun ApplicationError.toResponse(): ApplicationErrorResponse =
        ApplicationErrorResponse(id, occurredAt, level, loggerName, correlationId, organizationId, message, stackTrace, requestPath, requestMethod, userId)

    private fun WebhookEventLog.toResponse(): WebhookEventResponse =
        WebhookEventResponse(id, receivedAt, source, outcome, httpStatus, correlationId, durationMs, errorMessage)

    private fun <T, R> Page<T>.toPageResponse(mapper: (T) -> R): PageResponse<R> =
        PageResponse(content.map(mapper), number, size, totalElements, totalPages)
}
