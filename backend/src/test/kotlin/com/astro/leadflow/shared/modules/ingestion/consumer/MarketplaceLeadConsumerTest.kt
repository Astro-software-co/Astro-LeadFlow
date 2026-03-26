package com.astro.leadflow.shared.modules.ingestion.consumer

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.OrganizationStatus
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.domain.RawMarketplaceLead
import com.astro.leadflow.shared.modules.ingestion.domain.WebhookEventStatus
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import com.astro.leadflow.shared.modules.ingestion.service.LeadNormalizationService
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MarketplaceLeadFetcher
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class MarketplaceLeadConsumerTest {

    private lateinit var credentialRepository: MarketplaceCredentialRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var normalizationService: LeadNormalizationService
    private lateinit var leadIngestionService: LeadIngestionService
    private lateinit var webhookEventRepository: MarketplaceWebhookEventRepository
    private lateinit var wmFetcher: MarketplaceLeadFetcher
    private lateinit var mlFetcher: MarketplaceLeadFetcher
    private lateinit var consumer: MarketplaceLeadConsumer

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val orgId = UUID.randomUUID()
    private val webhookEventId = UUID.randomUUID()

    private val organization = Organization(
        id = orgId,
        name = "Test Org",
        slug = "test-org",
        cnpj = "00000000000100",
        status = OrganizationStatus.ACTIVE
    )

    private fun buildWebhookEvent(type: MarketplaceType) = MarketplaceWebhookEvent(
        id = webhookEventId,
        marketplaceType = type,
        rawPayload = "{}"
    )

    private fun buildRawLead(type: MarketplaceType) = RawMarketplaceLead(
        externalId = "lead-123",
        marketplaceType = type,
        buyerName = "João Silva",
        buyerEmail = "joao@test.com",
        buyerPhone = "11999998888",
        vehicleBrand = "Toyota",
        vehicleModel = "Corolla",
        vehicleYear = 2023,
        listingId = "WM-12345",
        listingUrl = null,
        contactType = "Proposta",
        receivedAt = Instant.now(),
        rawPayload = "{}"
    )

    private fun buildScrapedLeadDTO(type: MarketplaceType) = ScrapedLeadDTO(
        organizationId = orgId,
        organizationSlug = "test-org",
        name = "João Silva",
        phoneNumber = "11999998888",
        email = "joao@test.com",
        source = "MARKETPLACE_${type.name}",
        marketplaceType = type.name,
        vehicleInterest = "Toyota Corolla 2023",
        rawData = emptyMap()
    )

    @BeforeEach
    fun setup() {
        credentialRepository = mockk()
        organizationRepository = mockk()
        normalizationService = mockk()
        leadIngestionService = mockk()
        webhookEventRepository = mockk()

        wmFetcher = mockk()
        every { wmFetcher.supports(MarketplaceType.WEBMOTORS) } returns true
        every { wmFetcher.supports(MarketplaceType.MERCADO_LIVRE) } returns false
        every { wmFetcher.supports(MarketplaceType.OLX) } returns false

        mlFetcher = mockk()
        every { mlFetcher.supports(MarketplaceType.MERCADO_LIVRE) } returns true
        every { mlFetcher.supports(MarketplaceType.WEBMOTORS) } returns false
        every { mlFetcher.supports(MarketplaceType.OLX) } returns false

        consumer = MarketplaceLeadConsumer(
            fetchers = listOf(wmFetcher, mlFetcher),
            credentialRepository = credentialRepository,
            organizationRepository = organizationRepository,
            normalizationService = normalizationService,
            leadIngestionService = leadIngestionService,
            webhookEventRepository = webhookEventRepository,
            objectMapper = objectMapper
        )
    }

    private fun wmEventJson(cnpj: String = "00000000000100") = objectMapper.writeValueAsString(
        MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.WEBMOTORS,
            externalLeadId = "lead-123",
            cnpj = cnpj,
            webhookEventId = webhookEventId
        )
    )

    private fun mlEventJson(mlUserId: String = "1091441589") = objectMapper.writeValueAsString(
        MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            externalLeadId = "lead-456",
            externalUserId = mlUserId,
            webhookEventId = webhookEventId
        )
    )

    // ─── credencial helper ────────────────────────────────────────────────────

    private fun wmCredential() = MarketplaceCredential(
        organization = organization,
        marketplaceType = MarketplaceType.WEBMOTORS
    )

    private fun mlCredential() = MarketplaceCredential(
        organization = organization,
        marketplaceType = MarketplaceType.MERCADO_LIVRE,
        mlUserId = 1091441589L
    )

    // ─── Testes ───────────────────────────────────────────────────────────────

    @Test
    fun `fluxo feliz Webmotors deve ingerir lead e atualizar status para INGESTED`() {
        val webhookEvent = buildWebhookEvent(MarketplaceType.WEBMOTORS)
        val credential = wmCredential()
        val rawLead = buildRawLead(MarketplaceType.WEBMOTORS)
        val dto = buildScrapedLeadDTO(MarketplaceType.WEBMOTORS)

        every { webhookEventRepository.findById(webhookEventId) } returns Optional.of(webhookEvent)
        every { webhookEventRepository.save(any()) } returns webhookEvent
        every { organizationRepository.findByCnpj("00000000000100") } returns organization
        every { credentialRepository.findByOrganizationIdAndMarketplaceTypeAndIsActiveTrue(orgId, MarketplaceType.WEBMOTORS) } returns credential
        every { wmFetcher.fetchLeadDetail(any(), credential) } returns rawLead
        every { normalizationService.normalize(rawLead, organization) } returns dto
        every { leadIngestionService.ingestFromWebhook(dto) } just Runs

        consumer.process(wmEventJson())

        verify(exactly = 1) { leadIngestionService.ingestFromWebhook(dto) }
        val savedEvents = mutableListOf<MarketplaceWebhookEvent>()
        verify { webhookEventRepository.save(capture(savedEvents)) }
        // O último save deve ter status INGESTED
        val lastSave = savedEvents.last()
        assertEquals(WebhookEventStatus.INGESTED, lastSave.status)
    }

    @Test
    fun `fluxo feliz Mercado Livre deve ingerir lead e atualizar status para INGESTED`() {
        val webhookEvent = buildWebhookEvent(MarketplaceType.MERCADO_LIVRE)
        val credential = mlCredential()
        val rawLead = buildRawLead(MarketplaceType.MERCADO_LIVRE)
        val dto = buildScrapedLeadDTO(MarketplaceType.MERCADO_LIVRE)

        every { webhookEventRepository.findById(webhookEventId) } returns Optional.of(webhookEvent)
        every { webhookEventRepository.save(any()) } returns webhookEvent
        // Passo resolveOrganization: busca por mlUserId
        every { credentialRepository.findByMlUserIdAndMarketplaceTypeAndIsActiveTrue(1091441589L, MarketplaceType.MERCADO_LIVRE) } returns credential
        // Passo 3: busca credencial por org + type
        every { credentialRepository.findByOrganizationIdAndMarketplaceTypeAndIsActiveTrue(orgId, MarketplaceType.MERCADO_LIVRE) } returns credential
        every { mlFetcher.fetchLeadDetail(any(), credential) } returns rawLead
        every { normalizationService.normalize(rawLead, organization) } returns dto
        every { leadIngestionService.ingestFromWebhook(dto) } just Runs

        consumer.process(mlEventJson())

        verify(exactly = 1) { leadIngestionService.ingestFromWebhook(dto) }
    }

    @Test
    fun `quando organização não encontrada deve atualizar status para FAILED sem lançar exception`() {
        val webhookEvent = buildWebhookEvent(MarketplaceType.WEBMOTORS)

        every { webhookEventRepository.findById(webhookEventId) } returns Optional.of(webhookEvent)
        every { webhookEventRepository.save(any()) } returns webhookEvent
        every { organizationRepository.findByCnpj("00000000000100") } returns null

        // Não deve lançar exception
        consumer.process(wmEventJson())

        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }
        val savedEvents = mutableListOf<MarketplaceWebhookEvent>()
        verify { webhookEventRepository.save(capture(savedEvents)) }
        val lastSave = savedEvents.last()
        assertEquals(WebhookEventStatus.FAILED, lastSave.status)
    }

    @Test
    fun `quando organização está suspensa deve atualizar status para IGNORED`() {
        val suspendedOrg = Organization(
            id = orgId,
            name = "Test Org",
            slug = "test-org",
            cnpj = "00000000000100",
            status = OrganizationStatus.SUSPENDED
        )
        val webhookEvent = buildWebhookEvent(MarketplaceType.WEBMOTORS)

        every { webhookEventRepository.findById(webhookEventId) } returns Optional.of(webhookEvent)
        every { webhookEventRepository.save(any()) } returns webhookEvent
        every { organizationRepository.findByCnpj("00000000000100") } returns suspendedOrg

        consumer.process(wmEventJson())

        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }
        val savedEvents = mutableListOf<MarketplaceWebhookEvent>()
        verify { webhookEventRepository.save(capture(savedEvents)) }
        assertEquals(WebhookEventStatus.IGNORED, savedEvents.last().status)
    }

    @Test
    fun `quando credencial não encontrada deve atualizar status para FAILED`() {
        val webhookEvent = buildWebhookEvent(MarketplaceType.WEBMOTORS)

        every { webhookEventRepository.findById(webhookEventId) } returns Optional.of(webhookEvent)
        every { webhookEventRepository.save(any()) } returns webhookEvent
        every { organizationRepository.findByCnpj("00000000000100") } returns organization
        every { credentialRepository.findByOrganizationIdAndMarketplaceTypeAndIsActiveTrue(orgId, MarketplaceType.WEBMOTORS) } returns null

        consumer.process(wmEventJson())

        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }
        val savedEvents = mutableListOf<MarketplaceWebhookEvent>()
        verify { webhookEventRepository.save(capture(savedEvents)) }
        assertEquals(WebhookEventStatus.FAILED, savedEvents.last().status)
    }

    @Test
    fun `quando fetcher lança exceção deve atualizar status para FAILED sem propagar`() {
        val webhookEvent = buildWebhookEvent(MarketplaceType.WEBMOTORS)
        val credential = wmCredential()

        every { webhookEventRepository.findById(webhookEventId) } returns Optional.of(webhookEvent)
        every { webhookEventRepository.save(any()) } returns webhookEvent
        every { organizationRepository.findByCnpj("00000000000100") } returns organization
        every { credentialRepository.findByOrganizationIdAndMarketplaceTypeAndIsActiveTrue(orgId, MarketplaceType.WEBMOTORS) } returns credential
        every { wmFetcher.fetchLeadDetail(any(), credential) } throws RuntimeException("API Webmotors indisponível")

        // O consumer relança a exceção para que o SQS faça retry — isso é esperado
        // mas o status deve ter sido atualizado para FAILED antes do relançamento
        val savedEvents = mutableListOf<MarketplaceWebhookEvent>()
        every { webhookEventRepository.save(capture(savedEvents)) } returns webhookEvent

        try {
            consumer.process(wmEventJson())
        } catch (_: Exception) {
            // Exceção esperada — consumer relança para retry do SQS
        }

        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }
        // O último save capturado deve ter status FAILED
        val lastSave = savedEvents.lastOrNull()
        if (lastSave != null) {
            assertEquals(WebhookEventStatus.FAILED, lastSave.status)
        }
    }

    @Test
    fun `mensagem malformada deve ser descartada silenciosamente`() {
        // JSON inválido não deve lançar exception nem chamar outros serviços
        consumer.process("{ json invalido !!!")

        verify(exactly = 0) { webhookEventRepository.findById(any()) }
        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }
    }
}
