package com.astro.leadflow.shared.modules.ingestion.service.marketplace

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class MercadoLivreLeadFetcherTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authService: MercadoLivreAuthService
    private lateinit var fetcher: MercadoLivreLeadFetcher

    private val organization = Organization(id = UUID.randomUUID(), name = "Test Org ML", slug = "test-org-ml")

    private fun buildCredential(mlUserId: Long = 1091441589L) = MarketplaceCredential(
        organization = organization,
        marketplaceType = MarketplaceType.MERCADO_LIVRE,
        mlAccessToken = "ml-token-valido",
        mlTokenExpiresAt = LocalDateTime.now().plusHours(1),
        mlUserId = mlUserId
    )

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        authService = mockk()
        fetcher = MercadoLivreLeadFetcher(
            authService = authService,
            objectMapper = ObjectMapper(),
            webClientBuilder = WebClient.builder(),
            baseUrl = mockWebServer.url("/").toString().trimEnd('/')
        )
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    private val mlLeadPayload = """
        {
          "id": "44115522",
          "item_id": "MLB4037459422",
          "created_at": "2024-10-15T00:00:00Z",
          "contact_type": "whatsapp",
          "external_id": "13864821",
          "status": "active",
          "buyer_id": 1091441589,
          "name": "Maria Oliveira",
          "email": "maria@email.com",
          "phone": "+55 11 98888-7777"
        }
    """.trimIndent()

    @Test
    fun `deve extrair leadId corretamente e chamar endpoint vis_leads`() {
        val leadId = "3f2dedf2-dfbd-4981-a726-40b13aa172ff"
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "ml-token-valido"

        mockWebServer.enqueue(MockResponse()
            .setBody(mlLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            externalLeadId = leadId,
            webhookEventId = UUID.randomUUID()
        )

        fetcher.fetchLeadDetail(event, credential)

        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("/vis/leads/$leadId"))
    }

    @Test
    fun `para contact_type whatsapp deve usar endpoint vis_leads`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "ml-token-valido"

        mockWebServer.enqueue(MockResponse()
            .setBody(mlLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            externalLeadId = "44115522",
            contactType = "whatsapp",
            webhookEventId = UUID.randomUUID()
        )

        fetcher.fetchLeadDetail(event, credential)

        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("/vis/leads/"))
    }

    @Test
    fun `para contact_type contact_request deve usar endpoint leads details`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "ml-token-valido"

        mockWebServer.enqueue(MockResponse()
            .setBody(mlLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            externalLeadId = "44115522",
            contactType = "contact_request",
            webhookEventId = UUID.randomUUID()
        )

        fetcher.fetchLeadDetail(event, credential)

        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("/leads/44115522/details"))
    }

    @Test
    fun `deve mapear resposta ML para RawMarketplaceLead corretamente`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "ml-token-valido"

        mockWebServer.enqueue(MockResponse()
            .setBody(mlLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            externalLeadId = "44115522",
            webhookEventId = UUID.randomUUID()
        )

        val result = fetcher.fetchLeadDetail(event, credential)

        assertEquals("44115522", result.externalId)
        assertEquals("Maria Oliveira", result.buyerName)
        assertEquals("maria@email.com", result.buyerEmail)
        assertEquals("+55 11 98888-7777", result.buyerPhone)
        assertEquals("whatsapp", result.contactType)
        assertEquals("MLB4037459422", result.listingId)
        assertEquals(MarketplaceType.MERCADO_LIVRE, result.marketplaceType)
    }

    @Test
    fun `fetchLeadsSince deve paginar corretamente e retornar todos os itens`() {
        val credential = buildCredential(mlUserId = 1091441589L)
        every { authService.getValidToken(credential) } returns "ml-token-valido"

        // Página 1: 50 itens (tamanho do limit)
        val items50 = (1..50).joinToString(",") { i ->
            """{"id":"lead-$i","name":"Lead $i","email":"lead$i@test.com","phone":"119999${i.toString().padStart(4,'0')}","contact_type":"whatsapp","item_id":"MLB$i"}"""
        }
        val page1 = """{"results":[$items50],"paging":{"total":75,"offset":0,"limit":50}}"""

        // Página 2: 25 itens (menos que limit → última página)
        val items25 = (51..75).joinToString(",") { i ->
            """{"id":"lead-$i","name":"Lead $i","email":"lead$i@test.com","phone":"119999${i.toString().padStart(4,'0')}","contact_type":"whatsapp","item_id":"MLB$i"}"""
        }
        val page2 = """{"results":[$items25],"paging":{"total":75,"offset":50,"limit":50}}"""

        mockWebServer.enqueue(MockResponse().setBody(page1).addHeader("Content-Type", "application/json"))
        mockWebServer.enqueue(MockResponse().setBody(page2).addHeader("Content-Type", "application/json"))

        val result = fetcher.fetchLeadsSince(credential, Instant.now().minusSeconds(3600 * 25))

        assertEquals(75, result.size)
    }

    @Test
    fun `quando access_token expirado deve chamar authService para refresh`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "novo-ml-token"

        mockWebServer.enqueue(MockResponse()
            .setBody(mlLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            externalLeadId = "44115522",
            webhookEventId = UUID.randomUUID()
        )

        fetcher.fetchLeadDetail(event, credential)

        verify(exactly = 1) { authService.getValidToken(credential) }
        val request = mockWebServer.takeRequest()
        assertEquals("Bearer novo-ml-token", request.getHeader("Authorization"))
    }
}
