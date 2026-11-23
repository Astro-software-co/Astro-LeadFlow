package com.astro.leadflow.shared.modules.chatbot.engine.mock

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.TokenUsage
import com.astro.leadflow.shared.modules.chatbot.engine.BedrockEngine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.chatbot.engine", name = ["mode"], havingValue = "mock", matchIfMissing = true)
class MockBedrockEngine(
    private val intentResolver: KeywordIntentResolver
) : BedrockEngine {

    override fun handle(input: BotInput): BotOutput {
        val intent = intentResolver.resolve(input.message).takeUnless { it == BotIntent.FALLBACK }
            ?: BotIntent.VEHICLE_INQUIRY
        val output = "Posso refinar isso para você: confirme modelo desejado, faixa de preço e melhor horário para atendimento."
        return BotOutput(
            message = output,
            intent = intent,
            engineUsed = EngineType.BEDROCK,
            tokenUsage = TokenUsage(
                inputTokens = estimateTokens(input.message),
                outputTokens = estimateTokens(output)
            )
        )
    }

    private fun estimateTokens(text: String): Int =
        maxOf(1, text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size)
}
