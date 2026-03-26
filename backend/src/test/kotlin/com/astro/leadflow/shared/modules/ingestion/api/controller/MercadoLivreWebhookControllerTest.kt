package com.astro.leadflow.shared.modules.ingestion.api.controller

import com.astro.leadflow.shared.modules.ingestion.api.requests.MercadoLivreWebhookRequest
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.infra.MarketplaceSqsProducer
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class MercadoLivreWebhookControllerTest {

    private lateinit var sqsProducer: MarketplaceSqsProducer
    private lateinit var webhookEventRepository: MarketplaceWebhookEventRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var controller: MarketplaceWebhookController

    private val webhookToken = "test-marketplace-token"

    private fun savedEvent(leadId: String) = MarketplaceWebhookEvent(
        id = UUID.randomUUID(),
        marketplaceType = MarketplaceType.MERCADO_LIVRE,
        externalLeadId = leadId,
        rawPayload = "{}"
    )

    private fun visLeadsPayload(
        leadId: String = "3f2dedf2-dfbd-4981-a726-40b13aa172ff",
        notificationId: String = "notif-001"
    ) = MercadoLivreWebhookRequest(
        notificationId = notificationId,
        resource = "/vis/leads/$leadId",
        userId = 1091441589L,
        topic = "vis_leads",
        applicationId = 123456789L,
        attempts = 1,
        sent = "2024-10-15T14:30:00Z"
    )

    @BeforeEach
    fun setup() {
        sqsProducer = mockk()
        webhookEventRepository = mockk()
        objectMapper = ObjectMapper()
        controller = MarketplaceWebhookController(sqsProducer, webhookEventRepository, objectMapper, webhookToken)
    }

    @Test
    fun `quando topic é vis_leads deve publicar no SQS e retornar 200`() {
        val leadUuid = "3f2dedf2-dfbd-4981-a726-40b13aa172ff"

        every { webhookEventRepository.save(any()) } returns savedEvent(leadUuid)
        every { sqsProducer.publishMarketplaceLead(any()) } returns "sqs-msg-id-456"

        val response = controller.receiveMercadoLivre(webhookToken, visLeadsPayload(leadUuid))

        assertEquals(HttpStatus.OK, response.statusCode)
        verify { sqsProducer.publishMarketplaceLead(match {
            it.marketplaceType == MarketplaceType.MERCADO_LIVRE &&
            it.externalLeadId == leadUuid &&
            it.externalUserId == "1091441589"
        }) }
    }

    @Test
    fun `quando topic desconhecido recebido deve retornar 200 sem publicar`() {
        val payload = MercadoLivreWebhookRequest(
            notificationId = "notif-002",
            resource = "/orders/123456",
            userId = 1091441589L,
            topic = "orders",
            applicationId = 123456789L,
            attempts = 1,
            sent = "2024-10-15T14:30:00Z"
        )

        val response = controller.receiveMercadoLivre(webhookToken, payload)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(exactly = 0) { sqsProducer.publishMarketplaceLead(any()) }
        verify(exactly = 0) { webhookEventRepository.save(any()) }
    }

    @Test
    fun `quando resource não é de vis_leads deve extrair apenas o último segmento`() {
        val leadId = "44115522"

        every { webhookEventRepository.save(any()) } returns savedEvent(leadId)
        every { sqsProducer.publishMarketplaceLead(any()) } returns "sqs-msg-id-789"

        controller.receiveMercadoLivre(webhookToken, visLeadsPayload(leadId))

        verify { sqsProducer.publishMarketplaceLead(match { it.externalLeadId == leadId }) }
    }

    @Test
    fun `quando SQS lança exceção deve retornar 200 assim mesmo`() {
        every { webhookEventRepository.save(any()) } returns savedEvent("some-uuid")
        every { sqsProducer.publishMarketplaceLead(any()) } throws RuntimeException("SQS indisponível")

        val response = controller.receiveMercadoLivre(webhookToken, visLeadsPayload("some-uuid", "notif-004"))

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `quando token ausente deve retornar 401`() {
        val response = controller.receiveMercadoLivre(null, visLeadsPayload())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verify(exactly = 0) { webhookEventRepository.save(any()) }
    }

    @Test
    fun `quando token inválido deve retornar 401`() {
        val response = controller.receiveMercadoLivre("wrong-token", visLeadsPayload())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verify(exactly = 0) { webhookEventRepository.save(any()) }
    }
}
