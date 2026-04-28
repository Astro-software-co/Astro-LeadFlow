package com.astro.leadflow.shared.modules.chatbot.domain.valueobject

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType

data class BotOutput(
    val message: String,
    val intent: BotIntent,
    val engineUsed: EngineType,
    val slotValues: Map<String, String> = emptyMap(),
    val tokenUsage: TokenUsage = TokenUsage()
) {
    val fallback: Boolean
        get() = intent == BotIntent.FALLBACK
}
