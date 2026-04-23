package com.astro.leadflow.shared.modules.saas.infra

import com.astro.leadflow.shared.exception.ExternalServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class AbacatePayClient(
    @param:Value("\${app.abacatepay.api-url:https://api.abacatepay.com/v2}") private val apiUrl: String,
    @param:Value("\${app.abacatepay.api-key:}") private val apiKey: String,
    webClientBuilder: WebClient.Builder,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val webClient = webClientBuilder
        .baseUrl(apiUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun createProduct(request: AbacateCreateProductRequest): AbacateProductResponse =
        post("/products/create", request, object : ParameterizedTypeReference<AbacateEnvelope<AbacateProductResponse>>() {})

    fun createCustomer(request: AbacateCreateCustomerRequest): AbacateCustomerResponse =
        post("/customers/create", request, object : ParameterizedTypeReference<AbacateEnvelope<AbacateCustomerResponse>>() {})

    fun createSubscriptionCheckout(request: AbacateCreateSubscriptionCheckoutRequest): AbacateCheckoutResponse =
        post(
            "/subscriptions/create",
            request,
            object : ParameterizedTypeReference<AbacateEnvelope<AbacateCheckoutResponse>>() {},
        )

    fun cancelSubscription(id: String): AbacateSubscriptionResponse =
        post(
            "/subscriptions/cancel",
            mapOf("id" to id),
            object : ParameterizedTypeReference<AbacateEnvelope<AbacateSubscriptionResponse>>() {},
        )

    private fun <T> post(
        path: String,
        payload: Any,
        type: ParameterizedTypeReference<AbacateEnvelope<T>>,
    ): T {
        ensureApiKey()
        try {
            val envelope = webClient.post()
                .uri(path)
                .headers { it.setBearerAuth(apiKey) }
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(type)
                .block()

            if (envelope?.success != true || envelope.data == null) {
                log.error("AbacatePay retornou envelope inválido. path={} payload={} envelope={}", path, payload, envelope)
                throw ExternalServiceException("AbacatePay", envelope?.error ?: "Resposta sem dados")
            }

            return envelope.data
        } catch (e: WebClientResponseException) {
            val body = e.responseBodyAsString
            log.error("AbacatePay erro HTTP {} em {}. body={}", e.statusCode.value(), path, body)
            throw ExternalServiceException("AbacatePay", "HTTP ${e.statusCode.value()} ao chamar $path")
        } catch (e: ExternalServiceException) {
            throw e
        } catch (e: Exception) {
            log.error("Falha inesperada ao chamar AbacatePay em {}: {}", path, e.message, e)
            throw ExternalServiceException("AbacatePay", e.message ?: "Falha inesperada")
        }
    }

    private fun ensureApiKey() {
        if (apiKey.isBlank()) {
            throw ExternalServiceException("AbacatePay", "API key não configurada")
        }
    }
}

data class AbacateEnvelope<T>(
    val data: T? = null,
    val success: Boolean = false,
    val error: String? = null,
)

data class AbacateCreateProductRequest(
    val externalId: String,
    val name: String,
    val price: Long,
    val currency: String = "BRL",
    val description: String? = null,
    val cycle: String? = null,
)

data class AbacateProductResponse(
    val id: String,
    val externalId: String? = null,
    val name: String,
    val price: Long,
    val cycle: String? = null,
)

data class AbacateCreateCustomerRequest(
    val email: String,
    val name: String? = null,
    val cellphone: String? = null,
    val taxId: String? = null,
    val zipCode: String? = null,
    val metadata: Map<String, Any?>? = null,
)

data class AbacateCustomerResponse(
    val id: String,
    val email: String,
    val name: String? = null,
)

data class AbacateCreateSubscriptionCheckoutRequest(
    val items: List<AbacateCheckoutItem>,
    val customerId: String? = null,
    val externalId: String? = null,
    val returnUrl: String? = null,
    val completionUrl: String? = null,
    val methods: List<String> = listOf("CARD"),
    val metadata: Map<String, Any?>? = null,
)

data class AbacateCheckoutItem(
    val id: String,
    val quantity: Int,
)

data class AbacateCheckoutResponse(
    val id: String,
    val externalId: String? = null,
    val url: String,
    val amount: Long? = null,
    val status: String? = null,
)

data class AbacateSubscriptionResponse(
    val id: String,
    val status: String? = null,
)
