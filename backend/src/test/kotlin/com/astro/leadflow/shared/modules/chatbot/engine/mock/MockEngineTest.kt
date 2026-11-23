package com.astro.leadflow.shared.modules.chatbot.engine.mock

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotInput
import com.astro.leadflow.shared.modules.chatbot.engine.TemplateEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MockEngineTest {

    private val resolver = KeywordIntentResolver()
    private val input = BotInput(UUID.randomUUID(), UUID.randomUUID(), "Quero financiar esse carro")

    @Test
    fun `mock lex returns deterministic intent and slots`() {
        val output = MockLexEngine(resolver).handle(input)

        assertEquals(BotIntent.FINANCING_SIMULATION, output.intent)
        assertEquals(EngineType.LEX, output.engineUsed)
        assertEquals(input.message, output.slotValues["financingMessage"])
    }

    @Test
    fun `mock lex emits canonical qualification slots`() {
        val message = BotInput(
            leadId = input.leadId,
            organizationId = input.organizationId,
            message = "Quero financiar um Civic, tenho 50 mil e compro hoje",
            sessionId = input.sessionId,
            slotValues = emptyMap()
        )

        val slots = MockLexEngine(resolver).handle(message).slotValues

        assertEquals("FINANCING", slots["paymentType"])
        assertEquals("IMEDIATO", slots["timeline"])
        assertEquals("50000", slots["budget"])
    }

    @Test
    fun `mock bedrock estimates token usage`() {
        val output = MockBedrockEngine(resolver).handle(input)

        assertEquals(EngineType.BEDROCK, output.engineUsed)
        assertTrue(output.tokenUsage.inputTokens > 0)
        assertTrue(output.tokenUsage.outputTokens > 0)
    }

    @Test
    fun `template engine returns fixed response`() {
        val output = TemplateEngine().handle(input)

        assertEquals(EngineType.TEMPLATE, output.engineUsed)
        assertEquals(BotIntent.FALLBACK, output.intent)
    }
}
