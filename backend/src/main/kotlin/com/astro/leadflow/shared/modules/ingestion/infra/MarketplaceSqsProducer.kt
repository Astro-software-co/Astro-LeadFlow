package com.astro.leadflow.shared.modules.ingestion.infra

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Component
class MarketplaceSqsProducer(
    private val objectMapper: ObjectMapper,
    private val sqsClientProvider: ObjectProvider<SqsClient>,
    @Value("\${app.aws.sqs.marketplace-queue:marketplace-leads.fifo}") private val queueUrl: String,
    @Value("\${app.aws.enabled:false}") private val awsEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /** Publica evento de lead de marketplace na fila SQS FIFO.
     *  Retorna o messageId SQS, ou null se AWS estiver desabilitado. */
    fun publishMarketplaceLead(event: MarketplaceLeadEvent): String? {
        if (!awsEnabled) {
            log.debug("AWS desabilitado. Publicação marketplace ignorada. eventId={}", event.eventId)
            return null
        }

        val sqsClient = sqsClientProvider.ifAvailable
        if (sqsClient == null) {
            log.warn("SqsClient não disponível. Publicação marketplace ignorada. eventId={}", event.eventId)
            return null
        }

        val messageBody = objectMapper.writeValueAsString(event)
        val groupId = event.marketplaceType.name

        val response = sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageGroupId(groupId)
                .messageDeduplicationId(event.eventId)
                .build()
        )

        log.debug("Evento marketplace publicado. eventId={} sqsMessageId={}", event.eventId, response.messageId())
        return response.messageId()
    }
}
