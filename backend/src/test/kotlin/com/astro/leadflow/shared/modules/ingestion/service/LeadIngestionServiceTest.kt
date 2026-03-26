package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.communication.domain.ChatMessage
import com.astro.leadflow.shared.modules.communication.domain.MessageDirection
import com.astro.leadflow.shared.modules.communication.domain.MessageStatus
import com.astro.leadflow.shared.modules.communication.domain.MessageType
import com.astro.leadflow.shared.modules.communication.service.MessageSenderService
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.infra.SqsProducer
import com.astro.leadflow.shared.modules.saas.service.UsageLimitService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class LeadIngestionServiceTest {

    private lateinit var leadRepository: LeadRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var normalizationService: NormalizationService
    private lateinit var sqsProducer: SqsProducer
    private lateinit var messageSenderService: MessageSenderService
    private lateinit var usageLimitService: UsageLimitService
    private lateinit var service: LeadIngestionService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(
        id = orgId,
        name = "Concessionária Test",
        slug = "test-org"
    )

    private fun dto(phone: String = "11999998888") = ScrapedLeadDTO(
        organizationId = orgId,
        organizationSlug = "test-org",
        name = "João Silva",
        phoneNumber = phone,
        email = "joao@test.com",
        source = "MARKETPLACE_WEBMOTORS",
        marketplaceType = "WEBMOTORS",
        vehicleInterest = "Toyota Corolla 2023",
        rawData = emptyMap()
    )

    private fun savedLead(duplicate: Boolean = false) = Lead(
        organization = organization,
        phoneNumber = "+5511999998888",
        name = "João Silva",
        origin = "WEBMOTORS",
        pipelineStage = PipelineStage.TALKING_TO_BOT,
        isDuplicate = duplicate
    )

    @BeforeEach
    fun setup() {
        leadRepository = mockk()
        organizationRepository = mockk()
        normalizationService = NormalizationService() // serviço real, sem lógica externa
        sqsProducer = mockk()
        messageSenderService = mockk()
        usageLimitService = mockk(relaxed = true)

        service = LeadIngestionService(
            leadRepository = leadRepository,
            organizationRepository = organizationRepository,
            normalizationService = normalizationService,
            sqsProducer = sqsProducer,
            messageSenderService = messageSenderService,
            usageLimitService = usageLimitService,
            awsEnabled = false  // processa localmente em testes
        )

        every { organizationRepository.findBySlug("test-org") } returns Optional.of(organization)
        val fakeLead = savedLead()
        val fakeChatMessage = ChatMessage(
            lead = fakeLead,
            organization = organization,
            direction = MessageDirection.OUTBOUND,
            content = "[Template: welcome_lead]",
            messageType = MessageType.TEMPLATE
        )
        every { messageSenderService.sendTemplate(any(), any(), any(), any()) } returns fakeChatMessage
    }

    // ─── Testes de lead duplicado (Bug 1) ─────────────────────────────────────

    @Test
    fun `quando lead duplicado deve salvar com isDuplicate true`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returns true

        val savedSlot = slot<Lead>()
        every { leadRepository.save(capture(savedSlot)) } returns savedLead(duplicate = true)

        service.ingestFromWebhook(dto())

        assertTrue(savedSlot.captured.isDuplicate)
    }

    @Test
    fun `quando lead duplicado deve disparar WhatsApp`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returns true
        every { leadRepository.save(any()) } returns savedLead(duplicate = true)

        service.ingestFromWebhook(dto())

        verify(exactly = 1) {
            messageSenderService.sendTemplate(
                lead = any(),
                organization = organization,
                templateName = "welcome_lead",
                marketplaceName = any()
            )
        }
    }

    @Test
    fun `quando lead duplicado deve incrementar usage counter global e marketplace`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returns true
        every { leadRepository.save(any()) } returns savedLead(duplicate = true)

        service.ingestFromWebhook(dto())

        verify(exactly = 1) {
            usageLimitService.incrementUsage(orgId, "WEBMOTORS")
        }
    }

    @Test
    fun `quando lead duplicado deve incrementar usage ANTES de enviar WhatsApp`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returns true
        every { leadRepository.save(any()) } returns savedLead(duplicate = true)

        val callOrder = mutableListOf<String>()
        every { usageLimitService.incrementUsage(any(), any()) } answers { callOrder.add("increment") }
        every { messageSenderService.sendTemplate(any(), any(), any(), any()) } answers {
            callOrder.add("sendTemplate")
            savedLead(duplicate = true).let {
                ChatMessage(
                    lead = it,
                    organization = organization,
                    direction = MessageDirection.OUTBOUND,
                    content = "[Template: welcome_lead]",
                    messageType = MessageType.TEMPLATE
                )
            }
        }

        service.ingestFromWebhook(dto())

        assertEquals(listOf("increment", "sendTemplate"), callOrder)
    }

    // ─── Testes de lead novo ──────────────────────────────────────────────────

    @Test
    fun `quando lead novo deve salvar com isDuplicate false`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returns false
        // processFromQueue também verifica existência
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returns false andThen false

        val savedSlot = slot<Lead>()
        every { leadRepository.save(capture(savedSlot)) } returns savedLead(duplicate = false)

        service.ingestFromWebhook(dto())

        assertFalse(savedSlot.captured.isDuplicate)
    }

    @Test
    fun `quando lead novo deve enviar mensagem de boas-vindas`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returnsMany listOf(false, false)
        every { leadRepository.save(any()) } returns savedLead()

        service.ingestFromWebhook(dto())

        verify(exactly = 1) {
            messageSenderService.sendTemplate(any(), any(), any(), any())
        }
    }

    @Test
    fun `quando erro no WhatsApp não deve impedir salvamento do lead`() {
        val normalizedPhone = "+5511999998888"
        every { leadRepository.existsByOrganizationIdAndPhoneNumber(orgId, normalizedPhone) } returnsMany listOf(false, false)
        every { leadRepository.save(any()) } returns savedLead()
        every { messageSenderService.sendTemplate(any(), any(), any(), any()) } throws RuntimeException("Meta API indisponível")
        // Nota: o cast para ChatMessage é necessário pois o compilador infere o tipo de retorno

        // Não deve lançar exception — lead já foi salvo
        assertDoesNotThrow {
            service.ingestFromWebhook(dto())
        }

        verify(exactly = 1) { leadRepository.save(any()) }
    }
}
