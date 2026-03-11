package com.astro.leadflow.shared.modules.analytics.api.responses

data class MonthlySalesPoint(
    val month: String,
    val sales: Long,
    val revenue: Double
)