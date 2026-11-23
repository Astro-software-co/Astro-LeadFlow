package com.astro.leadflow.shared.modules.monitoring.controller.dto

data class ChatbotInsightsResponse(
    val totalSessions: Long,
    val globalFallbackRatePercent: Double,
    val tokensConsumed: Long,
    val topOrganizationsByTokens: List<OrganizationTokenUsageResponse>,
    val averageLatencyByEngineMs: Map<String, Double>
)
