package com.astro.leadflow.modules.communication.infra

import com.astro.leadflow.shared.modules.ingestion.api.requests.ScrapedLeadDTO
import com.astro.leadflow.modules.ingestion.service.LeadIngestionService
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SqsConsumer(
    private val leadIngestionService: LeadIngestionService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @SqsListener("\${aws.sqs.new-leads-queue-url}")
    fun onNewLead(message: String) {
        try {
            val lead = objectMapper.readValue(message, ScrapedLeadDTO::class.java)
            log.info("Processando lead da fila: ${lead.phoneNumber} | ${lead.marketplace}")
            leadIngestionService.processFromQueue(lead)
        } catch (e: Exception) {
            log.error("Erro ao processar mensagem da fila SQS: ${e.message}", e)
            // Re-throw para SQS mover para DLQ após N tentativas
            throw e
        }
    }
}