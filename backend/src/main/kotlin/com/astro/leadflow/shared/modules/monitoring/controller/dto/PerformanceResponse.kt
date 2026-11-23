package com.astro.leadflow.shared.modules.monitoring.controller.dto

data class PerformanceResponse(
    val requestsPerMinute: List<MetricPointResponse>,
    val errorRatePercent: List<MetricPointResponse>,
    val p50LatencyMs: List<MetricPointResponse>,
    val p95LatencyMs: List<MetricPointResponse>,
    val p99LatencyMs: List<MetricPointResponse>
)
