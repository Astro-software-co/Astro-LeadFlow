package com.astro.leadflow.shared.modules.ingestion.consumer

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.domain.WebhookEventStatus
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import com.astro.leadflow.shared.modules.ingestion.service.LeadNormalizationService
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MarketplaceLeadFetcher
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(prefix = "app.aws", name = ["enabled"], havingValue = "true")
class MarketplaceLeadConsumer(
    private val fetchers: List<MarketplaceLeadFetcher>,
    private val credentialRepository: MarketplaceCredentialRepository,
    private val normalizationService: LeadNormalizationService,
    private val leadIngestionService: LeadIngestionService,
    private val webhookEventRepository: MarketplaceWebhookEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @SqsListener("\${app.aws.sqs.marketplace-queue:marketplace-leads.fifo}")
    @Transactional
    fun process(message: String) {
        val event = runCatching {
            objectMapper.readValue(message, MarketplaceLeadEvent::class.java)
        }.getOrElse { e ->
            log.error("Mensagem de marketplace inválida, descartando. error={}", e.message)
            return  // sem throw — não pausa a fila por mensagem malformada
        }

        val webhookEvent = webhookEventRepository.findById(event.webhookEventId).orElse(null)
        webhookEvent?.status = WebhookEventStatus.PROCESSING
        webhookEvent?.let { webhookEventRepository.save(it) }

        try {
            // 1. Resolver organização
            val organization = resolveOrganization(event)
            if (organization == null) {
                log.warn("Organização não encontrada para evento marketplace. eventId={} type={}", event.eventId, event.marketplaceType)
                webhookEvent?.status = WebhookEventStatus.FAILED
                webhookEvent?.errorMessage = "Organização não encontrada"
                webhookEvent?.let { webhookEventRepository.save(it) }
                return
            }

            webhookEvent?.organization = organization

            // 2. Verificar se org está ativa
            if (organization.status.name == "SUSPENDED" || organization.status.name == "CANCELLED") {
                log.info("Org inativa — lead marketplace ignorado. org={} status={}", organization.slug, organization.status)
                webhookEvent?.status = WebhookEventStatus.IGNORED
                webhookEvent?.let { webhookEventRepository.save(it) }
                return
            }

            // 3. Buscar credencial
            val credential = credentialRepository.findByOrganizationIdAndMarketplaceTypeAndIsActiveTrue(
                organization.id, event.marketplaceType
            )
            if (credential == null) {
                log.warn("Credencial marketplace não encontrada. org={} type={}", organization.slug, event.marketplaceType)
                webhookEvent?.status = WebhookEventStatus.FAILED
                webhookEvent?.errorMessage = "Credencial não configurada"
                webhookEvent?.let { webhookEventRepository.save(it) }
                return
            }

            // 4. Buscar dados completos do lead via fetcher
            val fetcher = fetchers.find { it.supports(event.marketplaceType) }
                ?: throw IllegalStateException("Nenhum fetcher disponível para ${event.marketplaceType}")

            val rawLead = fetcher.fetchLeadDetail(event, credential)

            // 5. Normalizar
            val dto = normalizationService.normalize(rawLead, organization)

            // 6. Ingerir (inclui check de quota e envio de WhatsApp)
            leadIngestionService.ingestFromWebhook(dto)

            webhookEvent?.status = WebhookEventStatus.INGESTED
            webhookEvent?.processedAt = LocalDateTime.now()
            webhookEvent?.let { webhookEventRepository.save(it) }

            log.info("Lead marketplace ingerido. type={} externalId={} org={}", event.marketplaceType, event.externalLeadId, organization.slug)

        } catch (e: Exception) {
            log.error("Erro ao processar lead marketplace. eventId={} error={}", event.eventId, e.message, e)
            webhookEvent?.status = WebhookEventStatus.FAILED
            webhookEvent?.errorMessage = e.message?.take(500)
            webhookEvent?.processedAt = LocalDateTime.now()
            webhookEvent?.let { webhookEventRepository.save(it) }
            throw e  // relança para que o SQS faça retry
        }
    }

    private fun resolveOrganization(event: MarketplaceLeadEvent) = when (event.marketplaceType) {
        MarketplaceType.MERCADO_LIVRE -> {
            val mlUserId = event.externalUserId?.toLongOrNull()
            if (mlUserId != null) {
                credentialRepository.findByMlUserIdAndMarketplaceTypeAndIsActiveTrue(mlUserId, MarketplaceType.MERCADO_LIVRE)
                    ?.organization
            } else null
        }
        // Só o Mercado Livre entra por webhook; a Webmotors usa polling (integrations/webmotors).
        else -> null
    }
}
