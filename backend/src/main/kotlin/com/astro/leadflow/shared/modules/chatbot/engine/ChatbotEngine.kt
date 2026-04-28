package com.astro.leadflow.shared.modules.chatbot.engine

import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput

interface ChatbotEngine {
    fun handle(input: BotInput): BotOutput
}
