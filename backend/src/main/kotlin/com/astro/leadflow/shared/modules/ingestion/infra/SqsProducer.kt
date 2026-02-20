package com.astro.leadflow.modules.ingestion.infra

import com.astro.leadflow.modules.ingestion.api.requests.ScrapedLeadDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Component
class SqsProducer(
    private val objectMapper: ObjectMapper,
    private val sqsClientProvider: ObjectProvider<SqsClient>,
    @Value("\${aws.sqs.new-leads-queue-url:}") private val queueUrl: String,
    @Value("\${app.aws.enabled:false}") private val awsEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Publica lead capturado pelo scraper na fila SQS.
     * O módulo communication consome e dispara a mensagem de marketing.
     */
    fun publishNewLead(lead: ScrapedLeadDTO) {
        if (!awsEnabled) {
            log.debug("AWS desabilitado. Publicacao em fila ignorada.")
            return
        }
        if (queueUrl.isBlank()) {
            log.warn("AWS habilitado, mas aws.sqs.new-leads-queue-url nao foi configurado.")
            return
        }
        val sqsClient = sqsClientProvider.ifAvailable
        if (sqsClient == null) {
            log.warn("AWS habilitado, mas bean SqsClient nao esta disponivel.")
            return
        }

        val messageBody = objectMapper.writeValueAsString(lead)

        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageGroupId(lead.organizationSlug)   // Para FIFO queue
                .messageDeduplicationId(
                    "${lead.organizationSlug}_${lead.phoneNumber}_${System.currentTimeMillis()}"
                )
                .build()
        )
    }
}
