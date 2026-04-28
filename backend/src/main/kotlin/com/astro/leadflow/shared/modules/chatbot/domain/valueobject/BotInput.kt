package com.astro.leadflow.shared.modules.chatbot.domain.valueobject

import java.util.UUID

data class BotInput(
    val leadId: UUID,
    val organizationId: UUID,
    val message: String,
    val sessionId: UUID? = null,
    val slotValues: Map<String, String> = emptyMap()
)
