package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.ingestion.domain.*
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MarketplaceLeadFetcher
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MarketplaceReconciliationJobTest {

    private lateinit var credentialRepository: MarketplaceCredentialRepository
    private lateinit var normalizationService: LeadNormalizationService
    private lateinit var leadIngestionService: LeadIngestionService
    private lateinit var webhookEventRepository: MarketplaceWebhookEventRepository
    private lateinit var reconciliationLogRepository: MarketplaceReconciliationLogRepository
    private lateinit var fetcher: MarketplaceLeadFetcher
    private lateinit var job: MarketplaceReconciliationJob

    private val organization = Organization(id = UUID.randomUUID(), name = "Test Org", slug = "test-org")
    private val credential = MarketplaceCredential(organization = organization, marketplaceType = MarketplaceType.MERCADO_LIVRE)

    @BeforeEach
    fun setup() {
        credentialRepository = mockk(relaxed = true)
        normalizationService = mockk()
        leadIngestionService = mockk(relaxed = true)
        webhookEventRepository = mockk(relaxed = true)
        reconciliationLogRepository = mockk(relaxed = true)
        fetcher = mockk()

        every { fetcher.supports(MarketplaceType.MERCADO_LIVRE) } returns true
        every { reconciliationLogRepository.save(any<MarketplaceReconciliationLog>()) } answers { firstArg() }
        every { credentialRepository.save(any<MarketplaceCredential>()) } answers { firstArg() }
        every { webhookEventRepository.save(any<MarketplaceWebhookEvent>()) } answers { firstArg() }

        job = MarketplaceReconciliationJob(
            credentialRepository = credentialRepository,
            fetchers = listOf(fetcher),
            normalizationService = normalizationService,
            leadIngestionService = leadIngestionService,
            webhookEventRepository = webhookEventRepository,
            reconciliationLogRepository = reconciliationLogRepository
        )
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun aRawLead(externalId: String = "ext-${UUID.randomUUID()}"): RawMarketplaceLead =
        RawMarketplaceLead(
            externalId = externalId,
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            buyerName = "João Silva",
            buyerEmail = "joao@test.com",
            buyerPhone = "5511999990000",
            vehicleBrand = "Toyota",
            vehicleModel = "Corolla",
            vehicleYear = 2023,
            listingId = "ML-123",
            listingUrl = "https://mercadolivre.com/ML-123",
            contactType = "question",
            receivedAt = Instant.now(),
            rawPayload = """{"id":"$externalId"}"""
        )

    private fun aScrapedLeadDTO(): ScrapedLeadDTO =
        ScrapedLeadDTO(
            organizationId = organization.id,
            organizationSlug = organization.slug,
            name = "João Silva",
            phoneNumber = "5511999990000",
            email = "joao@test.com",
            source = "MARKETPLACE_MERCADO_LIVRE",
            marketplaceType = "MERCADO_LIVRE",
            vehicleInterest = "Toyota Corolla 2023",
            rawData = emptyMap()
        )

    // ─── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `should not fetch or ingest when there are no active Mercado Livre credentials`() {
        // Given
        every {
            credentialRepository.findAllByMarketplaceTypeAndIsActiveTrue(MarketplaceType.MERCADO_LIVRE)
        } returns emptyList()

        // When
        job.reconcile()

        // Then
        verify(exactly = 0) { fetcher.fetchLeadsSince(any(), any()) }
        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }
        verify(exactly = 0) { reconciliationLogRepository.save(any()) }
    }

    @Test
    fun `should set leadsAlreadySeen and status SUCCESS when all leads were already seen`() {
        // Given
        val lead1 = aRawLead("ext-1")
        val lead2 = aRawLead("ext-2")

        every {
            credentialRepository.findAllByMarketplaceTypeAndIsActiveTrue(MarketplaceType.MERCADO_LIVRE)
        } returns listOf(credential)
        every { fetcher.fetchLeadsSince(credential, any()) } returns listOf(lead1, lead2)
        every {
            webhookEventRepository.existsByExternalLeadIdAndMarketplaceType("ext-1", MarketplaceType.MERCADO_LIVRE)
        } returns true
        every {
            webhookEventRepository.existsByExternalLeadIdAndMarketplaceType("ext-2", MarketplaceType.MERCADO_LIVRE)
        } returns true

        val savedLog = slot<MarketplaceReconciliationLog>()
        every { reconciliationLogRepository.save(capture(savedLog)) } answers { firstArg() }

        // When
        job.reconcile()

        // Then
        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }

        val finalLog = savedLog.captured
        assertEquals(2, finalLog.leadsFound)
        assertEquals(2, finalLog.leadsAlreadySeen)
        assertEquals(0, finalLog.leadsNew)
        assertEquals("SUCCESS", finalLog.status)
        assertNotNull(finalLog.finishedAt)
    }

    @Test
    fun `should ingest new leads and set leadsNew count when new leads are found`() {
        // Given
        val seenLead = aRawLead("ext-seen")
        val newLead1 = aRawLead("ext-new-1")
        val newLead2 = aRawLead("ext-new-2")
        val dto = aScrapedLeadDTO()

        every {
            credentialRepository.findAllByMarketplaceTypeAndIsActiveTrue(MarketplaceType.MERCADO_LIVRE)
        } returns listOf(credential)
        every { fetcher.fetchLeadsSince(credential, any()) } returns listOf(seenLead, newLead1, newLead2)
        every {
            webhookEventRepository.existsByExternalLeadIdAndMarketplaceType("ext-seen", MarketplaceType.MERCADO_LIVRE)
        } returns true
        every {
            webhookEventRepository.existsByExternalLeadIdAndMarketplaceType("ext-new-1", MarketplaceType.MERCADO_LIVRE)
        } returns false
        every {
            webhookEventRepository.existsByExternalLeadIdAndMarketplaceType("ext-new-2", MarketplaceType.MERCADO_LIVRE)
        } returns false
        every { normalizationService.normalize(any(), organization) } returns dto

        val savedLogs = mutableListOf<MarketplaceReconciliationLog>()
        every { reconciliationLogRepository.save(capture(savedLogs)) } answers { firstArg() }

        // When
        job.reconcile()

        // Then
        verify(exactly = 2) { leadIngestionService.ingestFromWebhook(dto) }
        verify(exactly = 2) { webhookEventRepository.save(match { it.status == WebhookEventStatus.INGESTED }) }

        val finalLog = savedLogs.last()
        assertEquals(3, finalLog.leadsFound)
        assertEquals(1, finalLog.leadsAlreadySeen)
        assertEquals(2, finalLog.leadsNew)
        assertEquals("SUCCESS", finalLog.status)
        assertNotNull(finalLog.finishedAt)
        assertNotNull(credential.lastReconciledAt)
        verify { credentialRepository.save(credential) }
    }

    @Test
    fun `should set FAILED status and save log when fetcher throws exception`() {
        // Given
        every {
            credentialRepository.findAllByMarketplaceTypeAndIsActiveTrue(MarketplaceType.MERCADO_LIVRE)
        } returns listOf(credential)
        every { fetcher.fetchLeadsSince(credential, any()) } throws RuntimeException("API timeout")

        val savedLogs = mutableListOf<MarketplaceReconciliationLog>()
        every { reconciliationLogRepository.save(capture(savedLogs)) } answers { firstArg() }

        // When
        job.reconcile()

        // Then
        verify(exactly = 0) { leadIngestionService.ingestFromWebhook(any()) }

        val finalLog = savedLogs.last()
        assertEquals("FAILED", finalLog.status)
        assertEquals("API timeout", finalLog.errorMessage)
        assertNotNull(finalLog.finishedAt)
    }

    @Test
    fun `should continue with remaining leads when individual lead ingestion fails`() {
        // Given
        val failingLead = aRawLead("ext-fail")
        val successLead = aRawLead("ext-ok")
        val dto = aScrapedLeadDTO()

        every {
            credentialRepository.findAllByMarketplaceTypeAndIsActiveTrue(MarketplaceType.MERCADO_LIVRE)
        } returns listOf(credential)
        every { fetcher.fetchLeadsSince(credential, any()) } returns listOf(failingLead, successLead)
        every {
            webhookEventRepository.existsByExternalLeadIdAndMarketplaceType(any(), MarketplaceType.MERCADO_LIVRE)
        } returns false

        every { normalizationService.normalize(failingLead, organization) } throws
            IllegalArgumentException("Lead ext-fail sem telefone — descartado")
        every { normalizationService.normalize(successLead, organization) } returns dto

        val savedLogs = mutableListOf<MarketplaceReconciliationLog>()
        every { reconciliationLogRepository.save(capture(savedLogs)) } answers { firstArg() }

        // When
        job.reconcile()

        // Then
        verify(exactly = 1) { leadIngestionService.ingestFromWebhook(dto) }

        val finalLog = savedLogs.last()
        assertEquals(2, finalLog.leadsFound)
        assertEquals(1, finalLog.leadsNew)
        assertEquals(0, finalLog.leadsAlreadySeen)
        assertEquals("SUCCESS", finalLog.status)
        assertNotNull(finalLog.finishedAt)
    }
}
