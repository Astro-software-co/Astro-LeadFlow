package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.CrmLeadIntakeAdapter
import com.astro.leadflow.shared.modules.integrations.webmotors.service.IncomingLead
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import java.util.Optional

class CrmLeadIntakeAdapterTest {

    private val leads = mockk<LeadRepository>()
    private val orgs = mockk<OrganizationRepository>()
    private val sender = mockk<MessageSenderService>()
    private val org = Organization(name = "Loja", slug = "loja")
    private val adapter = CrmLeadIntakeAdapter(leads, orgs, sender, mockk<PlatformTransactionManager>(relaxed = true))

    private fun incoming(welcome: Boolean) = IncomingLead(
        name = "Maria", phone = "+5511987654321", email = null, origin = "WEBMOTORS",
        vehicleInterest = "HB20", notes = "Webmotors #1", sendWelcomeMessage = welcome
    )

    @BeforeEach
    fun setup() {
        every { orgs.findById(org.id) } returns Optional.of(org)
        every { leads.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(org.id, "+5511987654321") } returns Optional.empty()
    }

    @Test
    fun `lead novo sem boas-vindas vai para aguardando atendimento`() {
        val saved = slot<Lead>()
        every { leads.save(capture(saved)) } answers { saved.captured }

        val result = adapter.intake(org.id, incoming(welcome = false))

        assertTrue(result.created)
        assertEquals(PipelineStage.WAITING_ATTENDANCE, saved.captured.pipelineStage)
        assertEquals("WEBMOTORS", saved.captured.origin)
        verify(exactly = 0) { sender.sendTemplate(any(), any(), any(), any()) }
    }

    @Test
    fun `lead novo com boas-vindas envia template e fica com o bot`() {
        val saved = slot<Lead>()
        every { leads.save(capture(saved)) } answers { saved.captured }
        every { leads.findById(any()) } answers { Optional.of(saved.captured) }
        every { sender.sendTemplate(any(), org, "welcome_lead", "WEBMOTORS") } returns mockk()

        adapter.intake(org.id, incoming(welcome = true))

        assertEquals(PipelineStage.TALKING_TO_BOT, saved.captured.pipelineStage)
        verify { sender.sendTemplate(any(), org, "welcome_lead", "WEBMOTORS") }
    }

    @Test
    fun `falha no envio de boas-vindas nao desfaz o lead`() {
        val saved = slot<Lead>()
        every { leads.save(capture(saved)) } answers { saved.captured }
        every { leads.findById(any()) } answers { Optional.of(saved.captured) }
        every { sender.sendTemplate(any(), any(), any(), any()) } throws IllegalStateException("sem token")

        val result = adapter.intake(org.id, incoming(welcome = true))

        assertTrue(result.created)
    }

    @Test
    fun `telefone ja existente nao duplica e acrescenta o interesse as notas`() {
        val existing = Lead(organization = org, phoneNumber = "+5511987654321", origin = "OLX", notes = "Nota antiga")
        every { leads.findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc(org.id, "+5511987654321") } returns Optional.of(existing)
        every { leads.save(existing) } returns existing

        val result = adapter.intake(org.id, incoming(welcome = true))

        assertFalse(result.created)
        assertEquals(existing.id, result.leadId)
        assertTrue(existing.notes!!.startsWith("Nota antiga"))
        assertTrue(existing.notes!!.contains("Webmotors #1"))
        verify(exactly = 0) { sender.sendTemplate(any(), any(), any(), any()) }
    }
}
