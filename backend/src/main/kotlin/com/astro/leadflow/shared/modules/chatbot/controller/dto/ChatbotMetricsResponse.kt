package com.astro.leadflow.shared.modules.chatbot.controller.dto

data class ChatbotMetricsResponse(
    val totalSessions: Long,
    val fallbackRatePercent: Double,
    val handoffRatePercent: Double,
    val tokensConsumed: Long,
    val topIntents: List<TopIntentResponse>
)
