package com.astro.leadflow.shared.modules.ingestion.service.marketplace

import com.astro.leadflow.shared.modules.ingestion.domain.MarketplaceLeadEvent
import com.astro.leadflow.shared.modules.ingestion.domain.RawMarketplaceLead
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class MercadoLivreLeadFetcher(
    private val authService: MercadoLivreAuthService,
    private val objectMapper: ObjectMapper,
    webClientBuilder: WebClient.Builder,
    @Value("\${marketplace.mercadolivre.base-url:https://api.mercadolibre.com}") private val baseUrl: String
) : MarketplaceLeadFetcher {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()

    // Tipos que usam o endpoint /vis/leads/{id}
    private val visLeadsTypes = setOf("whatsapp", "call", "question", "visit_request")

    override fun supports(type: MarketplaceType) = type == MarketplaceType.MERCADO_LIVRE

    override fun fetchLeadDetail(event: MarketplaceLeadEvent, credential: MarketplaceCredential): RawMarketplaceLead {
        val token = authService.getValidToken(credential)
        val leadId = event.externalLeadId

        val responseBody = when {
            event.contactType == null || event.contactType in visLeadsTypes ->
                fetchFromVisLeads(leadId, token)
            else ->
                fetchFromLeadsDetails(leadId, token)
        }

        val rawJson = objectMapper.writeValueAsString(responseBody)
        log.debug("Lead ML {} buscado com sucesso", leadId)

        return RawMarketplaceLead(
            externalId = responseBody["id"]?.toString() ?: leadId,
            marketplaceType = MarketplaceType.MERCADO_LIVRE,
            buyerName = responseBody["name"] as? String,
            buyerEmail = responseBody["email"] as? String,
            buyerPhone = responseBody["phone"] as? String,
            vehicleBrand = null,
            vehicleModel = null,
            vehicleYear = null,
            listingId = responseBody["item_id"] as? String,
            listingUrl = null,
            contactType = responseBody["contact_type"] as? String ?: event.contactType,
            receivedAt = event.receivedAt,
            rawPayload = rawJson
        )
    }

    override fun fetchLeadsSince(credential: MarketplaceCredential, since: Instant): List<RawMarketplaceLead> {
        val token = authService.getValidToken(credential)
        val mlUserId = credential.mlUserId
            ?: run {
                log.warn("mlUserId não configurado para credencial ML id={}", credential.id)
                return emptyList()
            }

        val dateFrom = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneId.of("UTC"))
            .format(since)

        val results = mutableListOf<RawMarketplaceLead>()
        var offset = 0
        val limit = 50

        while (true) {
            val responseBody = webClient.get()
                .uri { builder ->
                    builder.path("/vis/users/$mlUserId/leads/buyers")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParam("date_from", dateFrom)
                        .queryParam("contact_types", "whatsapp,call,question,visit_request,contact_request")
                        .build()
                }
                .header("Authorization", "Bearer $token")
                .retrieve()
                .bodyToMono<Map<String, Any?>>()
                .block() ?: break

            @Suppress("UNCHECKED_CAST")
            val items = responseBody["results"] as? List<Map<String, Any?>> ?: break

            items.forEach { item ->
                val rawJson = objectMapper.writeValueAsString(item)
                results.add(
                    RawMarketplaceLead(
                        externalId = item["id"]?.toString() ?: "",
                        marketplaceType = MarketplaceType.MERCADO_LIVRE,
                        buyerName = item["name"] as? String,
                        buyerEmail = item["email"] as? String,
                        buyerPhone = item["phone"] as? String,
                        vehicleBrand = null,
                        vehicleModel = null,
                        vehicleYear = null,
                        listingId = item["item_id"] as? String,
                        listingUrl = null,
                        contactType = item["contact_type"] as? String,
                        receivedAt = Instant.now(),
                        rawPayload = rawJson
                    )
                )
            }

            if (items.size < limit) break
            offset += limit
        }

        return results
    }

    private fun fetchFromVisLeads(leadId: String, token: String): Map<String, Any?> {
        return webClient.get()
            .uri("/vis/leads/$leadId")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono<Map<String, Any?>>()
            .block() ?: throw IllegalStateException("Resposta nula ao buscar lead ML vis/leads id=$leadId")
    }

    private fun fetchFromLeadsDetails(leadId: String, token: String): Map<String, Any?> {
        return webClient.get()
            .uri("/leads/$leadId/details")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono<Map<String, Any?>>()
            .block() ?: throw IllegalStateException("Resposta nula ao buscar lead ML leads/details id=$leadId")
    }
}
