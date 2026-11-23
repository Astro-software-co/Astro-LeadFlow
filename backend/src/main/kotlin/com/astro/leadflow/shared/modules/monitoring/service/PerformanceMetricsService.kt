package com.astro.leadflow.shared.modules.monitoring.service

import com.astro.leadflow.shared.modules.monitoring.controller.dto.MetricPointResponse
import com.astro.leadflow.shared.modules.monitoring.controller.dto.PerformanceResponse
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PerformanceMetricsService(
    private val meterRegistry: MeterRegistry,
    private val metricsEndpoint: MetricsEndpoint
) {
    fun overviewRequestsPerMinute(): Double =
        meterRegistry.find("http.server.requests").timer()?.count()?.toDouble() ?: 0.0

    fun overviewErrorRatePercent(): Double {
        val total = meterRegistry.find("http.server.requests").timer()?.count()?.toDouble() ?: 0.0
        val serverErrors = runCatching {
            metricsEndpoint.metric("http.server.requests", listOf("status:500"))
                .measurements.firstOrNull()?.value ?: 0.0
        }.getOrDefault(0.0)
        return if (total <= 0.0) 0.0 else serverErrors * 100.0 / total
    }

    fun overviewP95LatencyMs(): Double =
        meterRegistry.find("http.server.requests").timer()?.takeSnapshot()?.percentileValues()
            ?.firstOrNull { it.percentile() == 0.95 }?.value(java.util.concurrent.TimeUnit.MILLISECONDS) ?: 0.0

    fun performance(): PerformanceResponse {
        val now = OffsetDateTime.now()
        return PerformanceResponse(
            requestsPerMinute = listOf(MetricPointResponse(now, overviewRequestsPerMinute())),
            errorRatePercent = listOf(MetricPointResponse(now, overviewErrorRatePercent())),
            p50LatencyMs = listOf(MetricPointResponse(now, 0.0)),
            p95LatencyMs = listOf(MetricPointResponse(now, overviewP95LatencyMs())),
            p99LatencyMs = listOf(MetricPointResponse(now, 0.0))
        )
    }
}
