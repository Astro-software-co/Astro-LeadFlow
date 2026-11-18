package com.astro.leadflow.shared.modules.communication.infra

import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.aws", name = ["enabled"], havingValue = "true")
class SqsConsumer(
    private val leadIngestionService: LeadIngestionService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @SqsListener("\${app.aws.sqs.lead-queue}")
    fun onNewLead(message: String) {
        try {
            val lead = objectMapper.readValue(message, ScrapedLeadDTO::class.java)
            log.info("Processando lead da fila: ${lead.phoneNumber} | ${lead.marketplaceType}")
            leadIngestionService.processFromQueue(lead)
        } catch (e: Exception) {
            log.error("Erro ao processar mensagem da fila SQS: ${e.message}", e)
            throw e
        }
    }
}