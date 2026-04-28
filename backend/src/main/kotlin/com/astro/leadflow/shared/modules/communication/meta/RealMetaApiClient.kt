package com.astro.leadflow.shared.modules.communication.meta

import com.astro.leadflow.shared.exception.ExternalServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
@ConditionalOnProperty(prefix = "meta.api", name = ["mode"], havingValue = "real")
class RealMetaApiClient(
    @param:Value("\${meta.api.base-url:https://graph.facebook.com/v18.0}") private val baseUrl: String,
    webClientBuilder: WebClient.Builder
) : MetaApiClient {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val webClient = webClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader("Content-Type", "application/json")
        .build()

    override fun sendText(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        message: String
    ): String {
        val payload = mapOf(
            "messaging_product" to "whatsapp",
            "recipient_type" to "individual",
            "to" to toPhone,
            "type" to "text",
            "text" to mapOf("body" to message)
        )
        return postMessage(phoneNumberId, accessToken, payload, toPhone)
    }

    override fun sendTemplate(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        templateName: String,
        languageCode: String,
        components: List<Map<String, Any>>
    ): String {
        val payload = mapOf(
            "messaging_product" to "whatsapp",
            "to" to toPhone,
            "type" to "template",
            "template" to mapOf(
                "name" to templateName,
                "language" to mapOf("code" to languageCode),
                "components" to components
            )
        )
        return postMessage(phoneNumberId, accessToken, payload, toPhone)
    }

    private fun postMessage(
        phoneNumberId: String,
        accessToken: String,
        payload: Map<String, Any>,
        toPhone: String
    ): String {
        try {
            val response = webClient.post()
                .uri("/$phoneNumberId/messages")
                .header("Authorization", "Bearer $accessToken")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<Map<String, Any>>() {})
                .block()

            @Suppress("UNCHECKED_CAST")
            val messages = response?.get("messages") as? List<Map<String, Any>>
            val messageId = messages?.firstOrNull()?.get("id") as? String
            if (messageId.isNullOrBlank()) {
                log.error("Meta API retornou resposta sem messageId. to={} response={}", toPhone, response)
                throw ExternalServiceException("Meta/WhatsApp", "Resposta sem messageId para $toPhone")
            }
            return messageId
        } catch (e: WebClientResponseException) {
            val status = e.statusCode.value()
            log.error("Meta API erro HTTP {} ao enviar para {}. body={}", status, toPhone, e.responseBodyAsString)
            throw ExternalServiceException("Meta/WhatsApp", "HTTP $status ao chamar /$phoneNumberId/messages")
        } catch (e: ExternalServiceException) {
            throw e
        } catch (e: Exception) {
            log.error("Erro inesperado ao enviar mensagem Meta para {}. error={}", toPhone, e.message, e)
            throw ExternalServiceException("Meta/WhatsApp", e.message ?: "Erro inesperado")
        }
    }
}
