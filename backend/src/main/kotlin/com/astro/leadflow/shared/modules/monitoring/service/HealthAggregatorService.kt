package com.astro.leadflow.shared.modules.monitoring.service

import com.astro.leadflow.shared.modules.monitoring.controller.dto.ObservabilityOverviewResponse
import org.springframework.boot.info.BuildProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory

@Service
class HealthAggregatorService(
    private val systemHealthService: SystemHealthService,
    private val performanceMetricsService: PerformanceMetricsService,
    private val buildProperties: ObjectProvider<BuildProperties>
) {
    fun overview(): ObservabilityOverviewResponse =
        ObservabilityOverviewResponse(
            uptimeSeconds = ManagementFactory.getRuntimeMXBean().uptime / 1000.0,
            health = systemHealthService.current(),
            requestsPerMinute = performanceMetricsService.overviewRequestsPerMinute(),
            errorRatePercent = performanceMetricsService.overviewErrorRatePercent(),
            p95LatencyMs = performanceMetricsService.overviewP95LatencyMs()
        )

    fun infrastructure(): Map<String, Any?> =
        mapOf(
            "health" to systemHealthService.current(),
            "version" to buildProperties.getIfAvailable()?.version,
            "name" to buildProperties.getIfAvailable()?.name
        )
}
