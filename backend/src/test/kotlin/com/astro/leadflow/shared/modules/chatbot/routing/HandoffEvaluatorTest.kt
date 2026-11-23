package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HandoffEvaluatorTest {

    private val evaluator = HandoffEvaluator()
    private val organization = Organization(name = "Astro", slug = "astro")
    private val lead = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "TEST")

    @Test
    fun `returns request human handoff`() {
        assertEquals(HandoffReason.REQUEST_HUMAN, evaluator.evaluate(session(), output(BotIntent.REQUEST_HUMAN)))
    }

    @Test
    fun `returns ready to buy handoff`() {
        assertEquals(HandoffReason.READY_TO_BUY, evaluator.evaluate(session(), output(BotIntent.READY_TO_BUY)))
    }

    @Test
    fun `returns fallback handoff after three consecutive fallbacks`() {
        assertEquals(HandoffReason.FALLBACK_LIMIT, evaluator.evaluate(session(fallbackCount = 3), output(BotIntent.FALLBACK)))
    }

    @Test
    fun `returns qualified handoff once lead is qualified`() {
        assertEquals(HandoffReason.QUALIFIED, evaluator.evaluate(session(), output(BotIntent.VEHICLE_INQUIRY), qualified = true))
    }

    @Test
    fun `keeps qualifying instead of handing off outside business hours`() {
        assertNull(evaluator.evaluate(session(), output(BotIntent.GREETING)))
    }

    @Test
    fun `does not handoff for normal intent`() {
        assertNull(evaluator.evaluate(session(), output(BotIntent.VEHICLE_INQUIRY)))
    }

    private fun session(fallbackCount: Int = 0): BotSession =
        BotSession(organization = organization, lead = lead).apply { this.fallbackCount = fallbackCount }

    private fun output(intent: BotIntent): BotOutput =
        BotOutput("ok", intent, EngineType.LEX)
}
