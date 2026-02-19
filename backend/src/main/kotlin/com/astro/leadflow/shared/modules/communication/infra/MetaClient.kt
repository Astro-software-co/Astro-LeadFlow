package com.astro.leadflow.modules.communication.infra

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class MetaClient(
    @Value("\${meta.api-url}") private val apiUrl: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(apiUrl)
        .defaultHeader("Content-Type", "application/json")
        .build()

    /**
     * Envia mensagem de texto simples para um número WhatsApp.
     * Retorna o waMessageId gerado pela Meta.
     */
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

        val response = webClient.post()
            .uri("/$phoneNumberId/messages")
            .header("Authorization", "Bearer $accessToken")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        @Suppress("UNCHECKED_CAST")
        val messages = response?.get("messages") as? List<Map<String, Any>>
        return messages?.firstOrNull()?.get("id") as? String
    }

    /**
     * Envia mensagem usando template aprovado pela Meta.
     * Necessário para iniciar conversas (janela 24h fechada).
     */
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

        val response = webClient.post()
            .uri("/$phoneNumberId/messages")
            .header("Authorization", "Bearer $accessToken")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        @Suppress("UNCHECKED_CAST")
        val messages = response?.get("messages") as? List<Map<String, Any>>
        return messages?.firstOrNull()?.get("id") as? String
    }
}