package com.astro.leadflow.shared.modules.chatbot.engine.mock

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.engine.LexEngine
import com.astro.leadflow.shared.modules.chatbot.qualification.LeadQualificationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.text.Normalizer

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

    private fun extractSlots(message: String, intent: BotIntent): Map<String, String> {
        val text = message.trim()
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        val slots = linkedMapOf<String, String>()

        when (intent) {
            BotIntent.VEHICLE_INQUIRY -> {
                slots["query"] = text
                slots[LeadQualificationService.VEHICLE] = text
            }
            BotIntent.FINANCING_SIMULATION -> slots["financingMessage"] = text
            else -> Unit
        }

        when {
            "a vista" in normalized -> slots[LeadQualificationService.PAYMENT_TYPE] = "CASH_PURCHASE"
            "troca" in normalized -> slots[LeadQualificationService.PAYMENT_TYPE] = "TRADE_IN"
            "financ" in normalized || intent == BotIntent.FINANCING_SIMULATION ->
                slots[LeadQualificationService.PAYMENT_TYPE] = "FINANCING"
        }

        when {
            Regex("\\b(hoje|agora|imediat\\w*|urgente|essa semana)\\b").containsMatchIn(normalized) ->
                slots[LeadQualificationService.TIMELINE] = "IMEDIATO"
            Regex("\\b(meses|ano|depois)\\b").containsMatchIn(normalized) ->
                slots[LeadQualificationService.TIMELINE] = "90_DIAS_OU_MAIS"
            Regex("\\b(mes|30 dias)\\b").containsMatchIn(normalized) ->
                slots[LeadQualificationService.TIMELINE] = "30_DIAS"
        }

        extractBudget(normalized)?.let { slots[LeadQualificationService.BUDGET] = it }
        return slots
    }

    private fun extractBudget(normalized: String): String? {
        val thousands = Regex("(\\d+)\\s*(mil|k)\\b").find(normalized)
        if (thousands != null) return (thousands.groupValues[1].toBigDecimal() * BigDecimal(1000)).toPlainString()
        val currency = Regex("r\\$\\s*(\\d{1,3}(?:\\.\\d{3})+|\\d+)").find(normalized) ?: return null
        return currency.groupValues[1].replace(".", "")
    }
}
