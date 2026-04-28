package com.astro.leadflow.shared.modules.monitoring.controller.dto

import java.time.OffsetDateTime

data class MetricPointResponse(
    val timestamp: OffsetDateTime,
    val value: Double
)
