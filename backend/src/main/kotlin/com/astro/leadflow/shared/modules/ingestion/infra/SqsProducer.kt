package com.astro.leadflow.modules.ingestion.infra

import com.astro.leadflow.shared.modules.ingestion.api.requests.ScrapedLeadDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Component
class SqsProducer(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.new-leads-queue-url}") private val queueUrl: String
) {

    /**
     * Publica lead capturado pelo scraper na fila SQS.
     * O módulo communication consome e dispara a mensagem de marketing.
     */
    fun publishNewLead(lead: ScrapedLeadDTO) {
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