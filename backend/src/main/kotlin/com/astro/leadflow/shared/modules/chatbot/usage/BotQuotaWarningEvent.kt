package com.astro.leadflow.shared.modules.chatbot.usage

import java.util.UUID

data class BotQuotaWarningEvent(
    val organizationId: UUID,
    val organizationName: String,
    val usedTokens: Int,
    val maxTokens: Int
)
