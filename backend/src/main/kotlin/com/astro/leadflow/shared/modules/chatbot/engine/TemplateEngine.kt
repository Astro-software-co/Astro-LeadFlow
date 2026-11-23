package com.astro.leadflow.shared.modules.chatbot.engine

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import org.springframework.stereotype.Component

@Component("chatbotTemplateEngine")
class TemplateEngine : ChatbotEngine {
    override fun handle(input: BotInput): BotOutput = BotOutput(
        message = "Olá! Recebemos sua mensagem. Um consultor da loja vai continuar o atendimento em breve.",
        intent = BotIntent.FALLBACK,
        engineUsed = EngineType.TEMPLATE
    )
}
