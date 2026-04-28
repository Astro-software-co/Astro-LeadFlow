package com.astro.leadflow.shared.modules.chatbot.engine.mock

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.LexEngine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.chatbot.engine", name = ["mode"], havingValue = "mock", matchIfMissing = true)
class MockLexEngine(
    private val intentResolver: KeywordIntentResolver
) : LexEngine {

    override fun handle(input: BotInput): BotOutput {
        val intent = intentResolver.resolve(input.message)
        return BotOutput(
            message = responseFor(intent),
            intent = intent,
            engineUsed = EngineType.LEX,
            slotValues = extractSlots(input.message, intent)
        )
    }

    private fun responseFor(intent: BotIntent): String = when (intent) {
        BotIntent.GREETING -> "Olá! Sou o assistente da Astro. Qual veículo você está buscando?"
        BotIntent.VEHICLE_INQUIRY -> "Temos algumas opções no estoque. Você procura modelo, ano ou faixa de preço específica?"
        BotIntent.SCHEDULE_TEST_DRIVE -> "Perfeito. Qual dia e horário você prefere para o test drive?"
        BotIntent.FINANCING_SIMULATION -> "Consigo ajudar com uma simulação. Qual valor de entrada você pretende usar?"
        BotIntent.BUSINESS_HOURS -> "Nosso atendimento funciona das 9h às 18h, no horário de São Paulo."
        BotIntent.REQUEST_HUMAN -> "Certo, vou chamar um consultor para continuar o atendimento."
        BotIntent.READY_TO_BUY -> "Ótimo! Vou encaminhar você para um consultor finalizar a compra."
        BotIntent.FALLBACK -> "Não entendi totalmente. Pode me dizer se quer ver estoque, simular financiamento ou agendar test drive?"
    }

    private fun extractSlots(message: String, intent: BotIntent): Map<String, String> =
        when (intent) {
            BotIntent.VEHICLE_INQUIRY -> mapOf("query" to message.trim())
            BotIntent.FINANCING_SIMULATION -> mapOf("financingMessage" to message.trim())
            else -> emptyMap()
        }
}
