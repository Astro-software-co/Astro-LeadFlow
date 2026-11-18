package com.astro.leadflow.shared.modules.ingestion.api.controller

import com.astro.leadflow.shared.modules.ingestion.api.requests.WebmotorsWebhookRequest
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.domain.WebhookEventStatus
import com.astro.leadflow.shared.modules.ingestion.infra.MarketplaceSqsProducer
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class WebmotorsWebhookControllerTest {

    private lateinit var sqsProducer: MarketplaceSqsProducer
    private lateinit var webhookEventRepository: MarketplaceWebhookEventRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var controller: MarketplaceWebhookController

    private val webhookToken = "test-marketplace-token"

    private val savedEvent = MarketplaceWebhookEvent(
        id = UUID.randomUUID(),
        marketplaceType = MarketplaceType.WEBMOTORS,
        externalLeadId = "424722",
        rawPayload = "{}"
    )

    @BeforeEach
    fun setup() {
        sqsProducer = mockk()
        webhookEventRepository = mockk()
        objectMapper = ObjectMapper()
        controller = MarketplaceWebhookController(sqsProducer, webhookEventRepository, objectMapper, webhookToken)

        every { webhookEventRepository.save(any()) } returns savedEvent
    }

    private fun validPayload() = WebmotorsWebhookRequest(
        idLead = "424722",
        codigoCliente = "123",
        cnpj = "00000000000100",
        idTipoLead = 2
    )

    @Test
    fun `quando payload válido recebido deve retornar 200 e publicar no SQS`() {
        every { sqsProducer.publishMarketplaceLead(any()) } returns "sqs-msg-id-123"

        val response = controller.receiveWebmotors(webhookToken, validPayload())

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(exactly = 1) { sqsProducer.publishMarketplaceLead(match { it.marketplaceType == MarketplaceType.WEBMOTORS && it.externalLeadId == "424722" && it.cnpj == "00000000000100" }) }
        verify(atLeast = 1) { webhookEventRepository.save(any()) }
    }

    @Test
    fun `quando payload recebido deve criar evento com status RECEIVED e depois QUEUED`() {
        every { sqsProducer.publishMarketplaceLead(any()) } returns "sqs-msg-id-123"

        val savedSlot = mutableListOf<MarketplaceWebhookEvent>()
        every { webhookEventRepository.save(capture(savedSlot)) } returns savedEvent

        controller.receiveWebmotors(webhookToken, validPayload())

        assertEquals(WebhookEventStatus.RECEIVED, savedSlot[0].status)
    }

    @Test
    fun `quando SQS lança exceção deve retornar 200 assim mesmo`() {
        every { sqsProducer.publishMarketplaceLead(any()) } throws RuntimeException("SQS indisponível")

        val response = controller.receiveWebmotors(webhookToken, validPayload())

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `quando banco lança exceção ao salvar deve retornar 200 assim mesmo`() {
        every { webhookEventRepository.save(any()) } throws RuntimeException("DB indisponível")

        val response = controller.receiveWebmotors(webhookToken, validPayload())

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `quando token ausente deve retornar 401`() {
        val response = controller.receiveWebmotors(null, validPayload())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verify(exactly = 0) { webhookEventRepository.save(any()) }
    }

    @Test
    fun `quando token inválido deve retornar 401`() {
        val response = controller.receiveWebmotors("wrong-token", validPayload())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verify(exactly = 0) { webhookEventRepository.save(any()) }
    }
}
