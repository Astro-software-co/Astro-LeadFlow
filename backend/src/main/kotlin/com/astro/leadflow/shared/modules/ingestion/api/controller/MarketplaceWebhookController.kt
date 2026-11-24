package com.astro.leadflow.shared.modules.ingestion.api.controller

import com.astro.leadflow.shared.modules.ingestion.api.requests.MercadoLivreWebhookRequest
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.domain.WebhookEventStatus
import com.astro.leadflow.shared.modules.ingestion.infra.MarketplaceSqsProducer
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Estratégia de autenticação: API Key estática por ambiente (header X-Webhook-Token).
 *
 * A Webmotors não usa webhook: entra por polling (módulo integrations/webmotors).
 *
 * Motivo: o Mercado Livre não documenta suporte a HMAC-SHA256 em seus
 * webhooks de leads. HMAC (opção a) não é viável. IP allowlist (opção c) é frágil
 * pois os IPs de origem podem mudar sem aviso. A API Key estática (opção b) é a
 * abordagem mais prática: simples de configurar no painel do marketplace e segura
 * o suficiente quando combinada com HTTPS.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Ingestion — Marketplaces", description = "Recebimento de leads via webhook dos marketplaces")
class MarketplaceWebhookController(
    private val sqsProducer: MarketplaceSqsProducer,
    private val webhookEventRepository: MarketplaceWebhookEventRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${marketplace.webhook-token:}") private val webhookToken: String
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val TOKEN_HEADER = "X-Webhook-Token"
    }

    @PostMapping("/mercadolivre")
    @Operation(summary = "Webhook Mercado Livre — recebe notificação de lead")
    fun receiveMercadoLivre(
        @RequestHeader(TOKEN_HEADER, required = false) token: String?,
        @RequestBody payload: MercadoLivreWebhookRequest
    ): ResponseEntity<Unit> {
        if (!validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return try {
            if (payload.topic != "vis_leads") {
                log.debug("Tópico ML ignorado: {}", payload.topic)
                return ResponseEntity.ok().build()
            }

            val rawJson = objectMapper.writeValueAsString(payload)
            val leadId = extractLeadIdFromResource(payload.resource)

            val event = webhookEventRepository.save(
                MarketplaceWebhookEvent(
                    marketplaceType = MarketplaceType.MERCADO_LIVRE,
                    externalLeadId = leadId,
                    rawPayload = rawJson
                )
            )

            val leadEvent = MarketplaceLeadEvent(
                marketplaceType = MarketplaceType.MERCADO_LIVRE,
                externalLeadId = leadId,
                externalUserId = payload.userId.toString(),
                webhookEventId = event.id
            )

            val msgId = sqsProducer.publishMarketplaceLead(leadEvent)

            event.status = WebhookEventStatus.QUEUED
            event.sqsMessageId = msgId
            webhookEventRepository.save(event)

            log.info("Webhook ML recebido e enfileirado. leadId={} userId={}", leadId, payload.userId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            log.error("Erro ao processar webhook ML. notificationId={} error={}", payload.notificationId, e.message, e)
            ResponseEntity.ok().build()
        }
    }

    private fun validateToken(token: String?): Boolean {
        if (webhookToken.isBlank()) {
            log.warn("marketplace.webhook-token não configurado — autenticação de webhook desabilitada")
            return true
        }
        if (token.isNullOrBlank()) {
            log.warn("Webhook marketplace recebido sem header {}", TOKEN_HEADER)
            return false
        }
        if (token != webhookToken) {
            log.warn("Webhook marketplace com token inválido")
            return false
        }
        return true
    }

    private fun extractLeadIdFromResource(resource: String): String {
        return resource.substringAfterLast("/")
    }
}
