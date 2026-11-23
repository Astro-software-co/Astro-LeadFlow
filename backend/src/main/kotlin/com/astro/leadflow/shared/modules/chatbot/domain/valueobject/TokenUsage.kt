package com.astro.leadflow.shared.modules.chatbot.domain.valueobject

data class TokenUsage(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0
) {
    val total: Int
        get() = inputTokens + outputTokens
}
