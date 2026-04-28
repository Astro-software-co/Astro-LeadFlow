package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import java.util.UUID

data class ChatbotResult(
    val message: String,
    val sessionId: UUID,
    val intent: BotIntent,
    val engineUsed: EngineType,
    val handoff: Boolean,
    val handoffReason: HandoffReason? = null
)
