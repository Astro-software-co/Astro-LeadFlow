package com.astro.leadflow.shared.modules.chatbot.controller.dto

import com.astro.leadflow.shared.modules.communication.api.responses.ChatMessageResponse

data class BotSessionDetailResponse(
    val session: BotSessionSummaryResponse,
    val slotValues: Map<String, String>,
    val messages: List<ChatMessageResponse>
)
