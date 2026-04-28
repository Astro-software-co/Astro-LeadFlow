package com.astro.leadflow.shared.modules.chatbot.routing

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.chatbot.session.BusinessHoursService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HandoffEvaluatorTest {

    private val businessHoursService = mockk<BusinessHoursService>()
    private val evaluator = HandoffEvaluator(businessHoursService)
    private val organization = Organization(name = "Astro", slug = "astro")
    private val lead = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "TEST")

    @Test
    fun `returns business hours handoff outside open window`() {
        every { businessHoursService.isOpenNow() } returns false

        assertEquals(HandoffReason.BUSINESS_HOURS, evaluator.evaluate(session(), output(BotIntent.GREETING)))
    }

    @Test
    fun `returns request human handoff`() {
        every { businessHoursService.isOpenNow() } returns true

        assertEquals(HandoffReason.REQUEST_HUMAN, evaluator.evaluate(session(), output(BotIntent.REQUEST_HUMAN)))
    }

    @Test
    fun `returns ready to buy handoff`() {
        every { businessHoursService.isOpenNow() } returns true

        assertEquals(HandoffReason.READY_TO_BUY, evaluator.evaluate(session(), output(BotIntent.READY_TO_BUY)))
    }

    @Test
    fun `returns fallback handoff after three consecutive fallbacks`() {
        every { businessHoursService.isOpenNow() } returns true

        assertEquals(HandoffReason.FALLBACK_LIMIT, evaluator.evaluate(session(fallbackCount = 3), output(BotIntent.FALLBACK)))
    }

    @Test
    fun `does not handoff for normal business hour intent`() {
        every { businessHoursService.isOpenNow() } returns true

        assertNull(evaluator.evaluate(session(), output(BotIntent.VEHICLE_INQUIRY)))
    }

    private fun session(fallbackCount: Int = 0): BotSession =
        BotSession(organization = organization, lead = lead).apply { this.fallbackCount = fallbackCount }

    private fun output(intent: BotIntent): BotOutput =
        BotOutput("ok", intent, EngineType.LEX)
}
