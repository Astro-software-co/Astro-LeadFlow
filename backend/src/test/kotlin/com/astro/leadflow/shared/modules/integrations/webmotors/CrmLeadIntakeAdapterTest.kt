package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.exception.ExternalServiceException
import com.astro.leadflow.shared.exception.QuotaExceededException
import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.intake.LeadIntakeCommand
import com.astro.leadflow.shared.modules.crm.intake.VehicleSnapshot
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.integrations.webmotors.infra.CrmLeadIntakeAdapter
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

/** O adaptador delega à regra oficial de entrada; aqui ela roda de verdade com repositórios simulados. */
class CrmLeadIntakeAdapterTest {

    private val leads = mockk<LeadRepository>()
    private val orgs = mockk<OrganizationRepository>()
    private val sender = mockk<MessageSenderService>()
    private val usage = mockk<UsageLimitService>(relaxed = true)
    private val org = Organization(name = "Loja", slug = "loja")
    private val ingestion = LeadIngestionService(
        leadRepository = leads, organizationRepository = orgs, normalizationService = NormalizationService(),
        sqsProducer = mockk<SqsProducer>(), messageSenderService = sender, usageLimitService = usage, awsEnabled = false
    )
    private val adapter = CrmLeadIntakeAdapter(orgs, ingestion)
    private val saved = slot<Lead>()

    private fun command(welcome: Boolean = true) = LeadIntakeCommand(
        organizationId = org.id, name = "Maria", phone = "+5511987654321", email = null, origin = "WEBMOTORS",
        externalEventId = "460046", listingId = "9988",
        vehicleSnapshot = VehicleSnapshot(brand = "Hyundai", model = "HB20", year = 2018),
        message = "Webmotors #460046", sendWelcome = welcome
    )

    @BeforeEach
    fun setup() {
        every { orgs.findById(org.id) } returns Optional.of(org)
        every { leads.existsByOrganizationIdAndPhoneNumber(org.id, "+5511987654321") } returns false
        every { leads.save(capture(saved)) } answers { saved.captured }
        every { sender.sendTemplate(any(), any(), any(), any()) } returns mockk()
    }

    @Test
    fun `lead novo e criado com a mensagem de abordagem`() {
        val result = adapter.intake(command())

        assertEquals(saved.captured.id, result.leadId)
        assertFalse(saved.captured.isDuplicate)
        assertEquals(PipelineStage.TALKING_TO_BOT, saved.captured.pipelineStage)
        assertEquals("Hyundai HB20 2018", saved.captured.vehicleInterest)
        assertEquals("Webmotors #460046", saved.captured.notes)
        verify(exactly = 1) { sender.sendTemplate(saved.captured, org, "welcome_lead", "WEBMOTORS") }
    }

    @Test
    fun `telefone repetido cria lead duplicado e ainda envia a mensagem`() {
        every { leads.existsByOrganizationIdAndPhoneNumber(org.id, "+5511987654321") } returns true

        adapter.intake(command())

        assertTrue(saved.captured.isDuplicate)
        verify(exactly = 1) { sender.sendTemplate(any(), org, "welcome_lead", "WEBMOTORS") }
    }

    @Test
    fun `cota excedida cria o lead sem mensagem`() {
        every { sender.sendTemplate(any(), any(), any(), any()) } throws QuotaExceededException()

        val result = adapter.intake(command())

        assertEquals(saved.captured.id, result.leadId)
        verify(exactly = 1) { leads.save(any()) }
    }

    @Test
    fun `falha da Meta nao desfaz o lead`() {
        every { sender.sendTemplate(any(), any(), any(), any()) } throws ExternalServiceException("Meta/WhatsApp", "HTTP 500")

        val result = adapter.intake(command())

        assertEquals(saved.captured.id, result.leadId)
        verify(exactly = 1) { leads.save(any()) }
    }

    @Test
    fun `sem abordagem o lead vai para atendimento e nada e enviado`() {
        adapter.intake(command(welcome = false))

        assertEquals(PipelineStage.WAITING_ATTENDANCE, saved.captured.pipelineStage)
        verify(exactly = 0) { sender.sendTemplate(any(), any(), any(), any()) }
    }
}
