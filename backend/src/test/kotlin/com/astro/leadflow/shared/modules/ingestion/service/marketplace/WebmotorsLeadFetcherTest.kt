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
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class WebmotorsLeadFetcherTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authService: WebmotorsAuthService
    private lateinit var fetcher: WebmotorsLeadFetcher

    private val organization = Organization(id = UUID.randomUUID(), name = "Test Org", slug = "test-org")

    private fun buildCredential(tokenExpiresAt: LocalDateTime = LocalDateTime.now().plusHours(1)) =
        MarketplaceCredential(
            organization = organization,
            marketplaceType = MarketplaceType.WEBMOTORS,
            wmAccessToken = "token-valido",
            wmTokenExpiresAt = tokenExpiresAt,
            wmIntegratorEmail = "integrador@test.com"
        )

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        authService = mockk()
        fetcher = WebmotorsLeadFetcher(
            authService = authService,
            objectMapper = ObjectMapper(),
            webClientBuilder = WebClient.builder(),
            baseUrl = mockWebServer.url("/").toString().trimEnd('/'),
            clientId = "test-client-id"
        )
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    private val wmLeadPayload = """
        {
          "IdLead": "424722",
          "NomeComprador": "João Silva",
          "EmailComprador": "joao@email.com",
          "TelefoneComprador": "11999998888",
          "MarcaVeiculo": "Toyota",
          "ModeloVeiculo": "Corolla",
          "AnoVeiculo": 2023,
          "CodigoAnuncio": "WM-12345",
          "UrlAnuncio": "https://www.webmotors.com.br/anuncio/12345",
          "DataLead": "2024-10-15T14:30:00Z",
          "StatusLead": 1
        }
    """.trimIndent()

    @Test
    fun `deve mapear resposta da API Webmotors para RawMarketplaceLead corretamente`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "token-valido"

        mockWebServer.enqueue(MockResponse()
            .setBody(wmLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.WEBMOTORS,
            externalLeadId = "424722",
            leadTypeId = 2,
            webhookEventId = UUID.randomUUID()
        )

        val result = fetcher.fetchLeadDetail(event, credential)

        assertEquals("424722", result.externalId)
        assertEquals("João Silva", result.buyerName)
        assertEquals("joao@email.com", result.buyerEmail)
        assertEquals("11999998888", result.buyerPhone)
        assertEquals("Toyota", result.vehicleBrand)
        assertEquals("Corolla", result.vehicleModel)
        assertEquals(2023, result.vehicleYear)
        assertEquals("WM-12345", result.listingId)
        assertEquals(MarketplaceType.WEBMOTORS, result.marketplaceType)
        assertEquals("Proposta", result.contactType)
    }

    @Test
    fun `quando token expirado deve chamar authService getValidToken antes do fetch`() {
        val expiredCredential = buildCredential(tokenExpiresAt = LocalDateTime.now().minusSeconds(10))
        every { authService.getValidToken(expiredCredential) } returns "novo-token"

        mockWebServer.enqueue(MockResponse()
            .setBody(wmLeadPayload)
            .addHeader("Content-Type", "application/json"))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.WEBMOTORS,
            externalLeadId = "424722",
            leadTypeId = 2,
            webhookEventId = UUID.randomUUID()
        )

        fetcher.fetchLeadDetail(event, expiredCredential)

        verify(exactly = 1) { authService.getValidToken(expiredCredential) }
        val request = mockWebServer.takeRequest()
        assertEquals("Bearer novo-token", request.getHeader("Authorization"))
    }

    @Test
    fun `quando API retorna 404 deve lançar exceção`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "token-valido"

        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.WEBMOTORS,
            externalLeadId = "999999",
            webhookEventId = UUID.randomUUID()
        )

        assertThrows(WebClientResponseException::class.java) {
            fetcher.fetchLeadDetail(event, credential)
        }
    }

    @Test
    fun `quando API retorna 500 deve lançar exceção`() {
        val credential = buildCredential()
        every { authService.getValidToken(credential) } returns "token-valido"

        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val event = MarketplaceLeadEvent(
            marketplaceType = MarketplaceType.WEBMOTORS,
            externalLeadId = "424722",
            webhookEventId = UUID.randomUUID()
        )

        assertThrows(WebClientResponseException::class.java) {
            fetcher.fetchLeadDetail(event, credential)
        }
    }

    @Test
    fun `fetchLeadsSince deve retornar lista vazia para Webmotors`() {
        val credential = buildCredential()

        val result = fetcher.fetchLeadsSince(credential, Instant.now().minusSeconds(3600))

        assertTrue(result.isEmpty())
        verify(exactly = 0) { authService.getValidToken(any()) }
    }
}
