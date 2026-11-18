package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceReconciliationLog
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceReconciliationLogRepository
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEvent
import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceWebhookEventRepository
import com.astro.leadflow.shared.modules.ingestion.domain.WebhookEventStatus
import com.astro.leadflow.shared.modules.ingestion.service.marketplace.MarketplaceLeadFetcher
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredentialRepository
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ConditionalOnProperty(
    name = ["marketplace.reconciliation.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@Component
class MarketplaceReconciliationJob(
    private val credentialRepository: MarketplaceCredentialRepository,
    private val fetchers: List<MarketplaceLeadFetcher>,
    private val normalizationService: LeadNormalizationService,
    private val leadIngestionService: LeadIngestionService,
    private val webhookEventRepository: MarketplaceWebhookEventRepository,
    private val reconciliationLogRepository: MarketplaceReconciliationLogRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /** Executa todo dia às 3h — reconcilia leads das últimas 25h (margem de segurança de 1h) */
    @Scheduled(cron = "\${marketplace.reconciliation.cron:0 0 3 * * *}")
    @Transactional
    fun reconcile() {
        log.info("Iniciando job de reconciliação de leads de marketplaces")
        val since = Instant.now().minus(25, ChronoUnit.HOURS)

        // Webmotors não tem endpoint de listagem — só ML
        val mlCredentials = credentialRepository.findAllByMarketplaceTypeAndIsActiveTrue(MarketplaceType.MERCADO_LIVRE)
        log.info("Reconciliando {} credenciais ML", mlCredentials.size)

        for (credential in mlCredentials) {
            val reconcileLog = reconciliationLogRepository.save(
                MarketplaceReconciliationLog(credential = credential)
            )

            try {
                val fetcher = fetchers.find { it.supports(MarketplaceType.MERCADO_LIVRE) }
                if (fetcher == null) {
                    log.warn("Nenhum fetcher ML disponível — reconciliação pulada")
                    break
                }

                val rawLeads = fetcher.fetchLeadsSince(credential, since)
                reconcileLog.leadsFound = rawLeads.size

                var newCount = 0
                var seenCount = 0

                for (rawLead in rawLeads) {
                    val alreadySeen = webhookEventRepository.existsByExternalLeadIdAndMarketplaceType(
                        rawLead.externalId, MarketplaceType.MERCADO_LIVRE
                    )

                    if (alreadySeen) {
                        seenCount++
                        continue
                    }

                    // Lead novo não visto pelo webhook — ingerir
                    try {
                        val dto = normalizationService.normalize(rawLead, credential.organization)
                        leadIngestionService.ingestFromWebhook(dto)

                        // Registrar como evento auditado
                        webhookEventRepository.save(
                            MarketplaceWebhookEvent(
                                marketplaceType = MarketplaceType.MERCADO_LIVRE,
                                organization = credential.organization,
                                externalLeadId = rawLead.externalId,
                                rawPayload = rawLead.rawPayload,
                                status = WebhookEventStatus.INGESTED,
                                processedAt = LocalDateTime.now()
                            )
                        )

                        newCount++
                        log.info("Lead ML reconciliado. externalId={} org={}", rawLead.externalId, credential.organization.slug)
                    } catch (e: Exception) {
                        log.warn("Falha ao ingerir lead na reconciliação. externalId={} error={}", rawLead.externalId, e.message)
                    }
                }

                reconcileLog.leadsNew = newCount
                reconcileLog.leadsAlreadySeen = seenCount
                reconcileLog.status = "SUCCESS"
                reconcileLog.finishedAt = LocalDateTime.now()

                // Atualizar timestamp de última reconciliação
                credential.lastReconciledAt = LocalDateTime.now()
                credentialRepository.save(credential)

                log.info("Reconciliação ML concluída. credentialId={} found={} new={} seen={}",
                    credential.id, rawLeads.size, newCount, seenCount)

            } catch (e: Exception) {
                reconcileLog.status = "FAILED"
                reconcileLog.errorMessage = e.message?.take(500)
                reconcileLog.finishedAt = LocalDateTime.now()
                log.error("Erro na reconciliação ML. credentialId={} error={}", credential.id, e.message, e)
            }

            reconciliationLogRepository.save(reconcileLog)
        }

        log.info("Job de reconciliação finalizado")
    }
}
