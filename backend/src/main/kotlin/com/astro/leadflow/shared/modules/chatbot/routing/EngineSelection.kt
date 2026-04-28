package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType

data class EngineSelection(
    val primary: EngineType,
    val allowBedrockFallback: Boolean
)
