package com.astro.leadflow.shared.modules.communication.infra

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class MetaClient(
    @param:Value("\${meta.api-url}") private val apiUrl: String,
    webClientBuilder: WebClient.Builder
) {
    private val webClient = webClientBuilder
        .baseUrl(apiUrl)
        .defaultHeader("Content-Type", "application/json")
        .build()

    fun sendTextMessage(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        message: String
    ): String? {
        val payload = mapOf(
            "messaging_product" to "whatsapp",
            "recipient_type" to "individual",
            "to" to toPhone,
            "type" to "text",
            "text" to mapOf("body" to message)
        )
        return postMessage(phoneNumberId, accessToken, payload)
    }

    fun sendTemplateMessage(
        phoneNumberId: String,
        accessToken: String,
        toPhone: String,
        templateName: String,
        languageCode: String = "pt_BR",
        components: List<Map<String, Any>> = emptyList()
    ): String? {
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
        return postMessage(phoneNumberId, accessToken, payload)
    }

    private fun postMessage(
        phoneNumberId: String,
        accessToken: String,
        payload: Map<String, Any>
    ): String? {
        return try {
            val response = webClient.post()
                .uri("/$phoneNumberId/messages")
                .header("Authorization", "Bearer $accessToken")
                .bodyValue(payload)
                .retrieve()
                .onStatus({ it.isError }) { resp ->
                    resp.bodyToMono<String>().map {
                        WebClientResponseException.create(
                            resp.statusCode().value(), it, null, null, null
                        )
                    }
                }
                .bodyToMono(Map::class.java)
                .block()

            @Suppress("UNCHECKED_CAST")
            val messages = response?.get("messages") as? List<Map<String, Any>>
            messages?.firstOrNull()?.get("id") as? String
        } catch (e: WebClientResponseException) {
            // Log e tratamento de erro da Meta (rate limit, token inválido, etc.)
            null
        }
    }
}