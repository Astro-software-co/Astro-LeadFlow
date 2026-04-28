package com.astro.leadflow.shared.modules.chatbot.session

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.chatbot.domain.valueobject.BotOutput
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.iam.domain.Organization
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Optional

class BotSessionServiceTest {
    private val repository = mockk<BotSessionRepository>()
    private val service = BotSessionService(repository)
    private val organization = Organization(name = "Astro", slug = "astro")
    private val lead = Lead(organization = organization, phoneNumber = "+5511999999999", origin = "TEST")

    @Test
    fun `getOrCreateActive returns existing active session`() {
        val session = BotSession(organization = organization, lead = lead)
        every { repository.findFirstByLeadIdAndStatusOrderByUpdatedAtDesc(lead.id, BotSessionStatus.ACTIVE) } returns Optional.of(session)

        assertEquals(session, service.getOrCreateActive(lead))
    }

    @Test
    fun `getOrCreateActive creates session when none exists`() {
        val saved = BotSession(organization = organization, lead = lead)
        every { repository.findFirstByLeadIdAndStatusOrderByUpdatedAtDesc(lead.id, BotSessionStatus.ACTIVE) } returns Optional.empty()
        every { repository.save(any()) } returns saved

        assertEquals(saved, service.getOrCreateActive(lead))
    }

    @Test
    fun `applyOutput updates intent engine slots and fallback count`() {
        val session = BotSession(organization = organization, lead = lead)
        every { repository.save(session) } returns session

        service.applyOutput(session, BotOutput("x", BotIntent.FALLBACK, EngineType.LEX, mapOf("a" to "b")))

        assertEquals(BotIntent.FALLBACK, session.currentIntent)
        assertEquals(EngineType.LEX, session.engineUsed)
        assertEquals(1, session.fallbackCount)
        assertEquals("b", session.slotValues["a"])
    }

    @Test
    fun `markHandoff changes session and lead stage`() {
        val session = BotSession(organization = organization, lead = lead)
        every { repository.save(session) } returns session

        service.markHandoff(session, HandoffReason.MANUAL)

        assertEquals(BotSessionStatus.HANDOFF, session.status)
        assertEquals(PipelineStage.WAITING_ATTENDANCE, lead.pipelineStage)
        verify { repository.save(session) }
    }
}
