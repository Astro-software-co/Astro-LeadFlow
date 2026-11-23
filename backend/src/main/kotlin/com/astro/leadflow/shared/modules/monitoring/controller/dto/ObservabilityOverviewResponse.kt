package com.astro.leadflow.shared.modules.monitoring.controller.dto

data class ObservabilityOverviewResponse(
    val uptimeSeconds: Double,
    val health: SystemHealthResponse,
    val requestsPerMinute: Double,
    val errorRatePercent: Double,
    val p95LatencyMs: Double
)
