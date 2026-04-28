package com.astro.leadflow.shared.modules.monitoring.controller.dto

data class WebhookSummaryItemResponse(
    val source: String,
    val outcome: String,
    val count: Long
)
