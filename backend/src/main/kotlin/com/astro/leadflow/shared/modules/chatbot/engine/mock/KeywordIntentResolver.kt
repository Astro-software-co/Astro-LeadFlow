package com.astro.leadflow.shared.modules.chatbot.engine.mock

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import org.springframework.stereotype.Component
import java.text.Normalizer

@Component
class KeywordIntentResolver {

    private val rules = listOf(
        BotIntent.REQUEST_HUMAN to listOf("humano", "atendente", "vendedor", "consultor", "pessoa", "falar com"),
        BotIntent.READY_TO_BUY to listOf("comprar", "fechar", "fechar negocio", "quero esse", "vou ficar", "reservar"),
        BotIntent.SCHEDULE_TEST_DRIVE to listOf("test drive", "teste drive", "dirigir", "agenda", "agendar", "visita"),
        BotIntent.FINANCING_SIMULATION to listOf("financiamento", "financiar", "parcela", "parcelas", "entrada", "simulacao", "credito"),
        BotIntent.BUSINESS_HOURS to listOf("horario", "abre", "fecha", "funciona", "atendimento", "expediente"),
        BotIntent.VEHICLE_INQUIRY to listOf("carro", "veiculo", "modelo", "estoque", "preco", "valor", "km", "quilometragem"),
        BotIntent.GREETING to listOf("oi", "ola", "olá", "bom dia", "boa tarde", "boa noite", "e ai")
    )

    fun resolve(message: String): BotIntent {
        val normalized = normalize(message)
        if (normalized.isBlank()) return BotIntent.FALLBACK
        return rules.firstOrNull { (_, keywords) ->
            keywords.any { keyword -> containsTokenOrPhrase(normalized, normalize(keyword)) }
        }?.first ?: BotIntent.FALLBACK
    }

    private fun containsTokenOrPhrase(message: String, keyword: String): Boolean {
        if (keyword.contains(' ')) return message.contains(keyword)
        return Regex("(^|\\W)$keyword(\\W|$)").containsMatchIn(message)
    }

    private fun normalize(value: String): String {
        val withoutAccents = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents.replace("[^a-z0-9 ]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
