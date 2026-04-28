package com.astro.leadflow.shared.modules.chatbot.controller.dto

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import java.time.OffsetDateTime
import java.util.UUID

data class BotSessionSummaryResponse(
    val id: UUID,
    val leadId: UUID,
    val leadName: String?,
    val leadPhone: String,
    val status: BotSessionStatus,
    val currentIntent: BotIntent?,
    val engineUsed: EngineType,
    val fallbackCount: Int,
    val handoffReason: HandoffReason?,
    val lastMessageAt: OffsetDateTime?,
    val createdAt: OffsetDateTime
)
