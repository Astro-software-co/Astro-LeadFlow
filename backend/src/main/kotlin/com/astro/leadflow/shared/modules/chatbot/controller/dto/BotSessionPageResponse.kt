package com.astro.leadflow.shared.modules.chatbot.controller.dto

data class BotSessionPageResponse(
    val content: List<BotSessionSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
