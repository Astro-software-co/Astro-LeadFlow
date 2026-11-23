package com.astro.leadflow.shared.modules.monitoring.service

import com.astro.leadflow.shared.modules.chatbot.session.BotSessionRepository
import com.astro.leadflow.shared.modules.chatbot.usage.AiTokenUsageRepository
import com.astro.leadflow.shared.modules.monitoring.controller.dto.ChatbotInsightsResponse
import org.springframework.stereotype.Service

@Service
class ChatbotInsightsService(
    private val botSessionRepository: BotSessionRepository,
    private val aiTokenUsageRepository: AiTokenUsageRepository
) {
    fun insights(): ChatbotInsightsResponse {
        val total = botSessionRepository.count()
        val fallback = 0L
        val denominator = total.takeIf { it > 0 } ?: 1
        return ChatbotInsightsResponse(
            totalSessions = total,
            globalFallbackRatePercent = fallback * 100.0 / denominator,
            tokensConsumed = aiTokenUsageRepository.count(),
            topOrganizationsByTokens = emptyList(),
            averageLatencyByEngineMs = emptyMap()
        )
    }
}
