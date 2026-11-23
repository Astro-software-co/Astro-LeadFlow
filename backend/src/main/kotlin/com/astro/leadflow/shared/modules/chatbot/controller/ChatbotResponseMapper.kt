package com.astro.leadflow.shared.modules.chatbot.controller

import com.astro.leadflow.shared.modules.chatbot.controller.dto.BotSessionSummaryResponse
import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import org.springframework.stereotype.Component

@Component
class ChatbotResponseMapper {
    fun toSummary(session: BotSession): BotSessionSummaryResponse =
        BotSessionSummaryResponse(
            id = session.id,
            leadId = session.lead.id,
            leadName = session.lead.name,
            leadPhone = session.lead.phoneNumber,
            status = session.status,
            currentIntent = session.currentIntent,
            engineUsed = session.engineUsed,
            fallbackCount = session.fallbackCount,
            handoffReason = session.handoffReason,
            lastMessageAt = session.lastMessageAt,
            createdAt = session.createdAt
        )
}
