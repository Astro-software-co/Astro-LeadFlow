package com.astro.leadflow.shared.modules.analytics.api.responses

data class KpiCard(
    val value: Double,
    val previousValue: Double,
    val unit: KpiUnit
) {
    val percentageChange: Double = if (previousValue == 0.0) 0.0
        else ((value - previousValue) / previousValue) * 100
}