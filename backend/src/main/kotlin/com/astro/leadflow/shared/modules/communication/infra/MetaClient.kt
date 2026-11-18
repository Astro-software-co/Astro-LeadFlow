package com.astro.leadflow.shared.modules.communication.infra

import com.astro.leadflow.shared.exception.ExternalServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class MetaClient(
    @param:Value("\${app.meta.api-url}") private val apiUrl: String,
    webClientBuilder: WebClient.Builder
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val webClient = webClientBuilder
        .baseUrl(apiUrl)
        .defaultHeader("Content-Type", "application/json")
        .build()

    fun sendTextMessage(
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

    fun sendTemplateMessage(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        templateName: String,
        languageCode: String = "pt_BR",
        components: List<Map<String, Any>> = emptyList()
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
                .bodyToMono(Map::class.java)
                .block()

            @Suppress("UNCHECKED_CAST")
            val messages = response?.get("messages") as? List<Map<String, Any>>
            val messageId = messages?.firstOrNull()?.get("id") as? String

            if (messageId == null) {
                log.error("Meta API retornou resposta sem messageId. to={} response={}", toPhone, response)
                throw ExternalServiceException("Meta/WhatsApp", "Resposta sem messageId para $toPhone")
            }

            log.debug("Mensagem enviada com sucesso. to={} messageId={}", toPhone, messageId)
            return messageId
        } catch (e: WebClientResponseException) {
            val status = e.statusCode.value()
            val body = e.responseBodyAsString
            log.error("Meta API erro HTTP {} ao enviar para {}. body={}", status, toPhone, body)

            val errorMessage = when (status) {
                401 -> "Token de acesso inválido ou expirado"
                429 -> "Rate limit excedido na Meta API"
                400 -> parseMetaErrorMessage(body) ?: "Requisição inválida"
                else -> "Erro HTTP $status"
            }

            throw ExternalServiceException("Meta/WhatsApp", "$errorMessage (to=$toPhone)")
        } catch (e: ExternalServiceException) {
            throw e
        } catch (e: Exception) {
            log.error("Erro inesperado ao enviar mensagem para {}. error={}", toPhone, e.message, e)
            throw ExternalServiceException("Meta/WhatsApp", "Erro inesperado: ${e.message}")
        }
    }

    private fun parseMetaErrorMessage(body: String): String? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val regex = """"message"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(body)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }
}