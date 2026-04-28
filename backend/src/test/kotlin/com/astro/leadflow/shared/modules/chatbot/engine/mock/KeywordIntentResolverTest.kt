package com.astro.leadflow.shared.modules.chatbot.engine.mock

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KeywordIntentResolverTest {

    private val resolver = KeywordIntentResolver()

    @Test
    fun `resolves greeting with accents and uppercase`() {
        assertEquals(BotIntent.GREETING, resolver.resolve("OLÁ, bom dia!"))
    }

    @Test
    fun `resolves vehicle inquiry`() {
        assertEquals(BotIntent.VEHICLE_INQUIRY, resolver.resolve("Qual o preço desse carro no estoque?"))
    }

    @Test
    fun `resolves test drive scheduling`() {
        assertEquals(BotIntent.SCHEDULE_TEST_DRIVE, resolver.resolve("Quero agendar um test drive"))
    }

    @Test
    fun `resolves financing simulation without accent`() {
        assertEquals(BotIntent.FINANCING_SIMULATION, resolver.resolve("Pode fazer uma simulacao de financiamento?"))
    }

    @Test
    fun `resolves business hours`() {
        assertEquals(BotIntent.BUSINESS_HOURS, resolver.resolve("Qual horario de atendimento?"))
    }

    @Test
    fun `resolves request human before generic attendance words`() {
        assertEquals(BotIntent.REQUEST_HUMAN, resolver.resolve("Quero falar com um atendente humano"))
    }

    @Test
    fun `resolves ready to buy before vehicle inquiry`() {
        assertEquals(BotIntent.READY_TO_BUY, resolver.resolve("Gostei do carro e quero comprar"))
    }

    @Test
    fun `falls back for blank or unrelated text`() {
        assertEquals(BotIntent.FALLBACK, resolver.resolve(""))
        assertEquals(BotIntent.FALLBACK, resolver.resolve("banana com azul"))
    }

    @Test
    fun `does not match partial token`() {
        assertEquals(BotIntent.FALLBACK, resolver.resolve("carroceria"))
    }
}
