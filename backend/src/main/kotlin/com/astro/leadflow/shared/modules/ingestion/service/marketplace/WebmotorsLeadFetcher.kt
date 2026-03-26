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

@Component
class WebmotorsLeadFetcher(
    private val authService: WebmotorsAuthService,
    private val objectMapper: ObjectMapper,
    webClientBuilder: WebClient.Builder,
    @Value("\${marketplace.webmotors.base-url:https://api-webmotors.sensedia.com}") private val baseUrl: String,
    @Value("\${marketplace.webmotors.client-id:}") private val clientId: String
) : MarketplaceLeadFetcher {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()

    override fun supports(type: MarketplaceType) = type == MarketplaceType.WEBMOTORS

    override fun fetchLeadDetail(event: MarketplaceLeadEvent, credential: MarketplaceCredential): RawMarketplaceLead {
        val token = authService.getValidToken(credential)

        val responseBody = webClient.get()
            .uri("/leads/v1/${event.externalLeadId}")
            .header("client_id", clientId)
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono<Map<String, Any?>>()
            .block() ?: throw IllegalStateException("Resposta nula ao buscar lead Webmotors id=${event.externalLeadId}")

        val rawJson = objectMapper.writeValueAsString(responseBody)
        log.debug("Lead Webmotors {} buscado com sucesso", event.externalLeadId)

        return RawMarketplaceLead(
            externalId = responseBody["IdLead"]?.toString() ?: event.externalLeadId,
            marketplaceType = MarketplaceType.WEBMOTORS,
            buyerName = responseBody["NomeComprador"] as? String,
            buyerEmail = responseBody["EmailComprador"] as? String,
            buyerPhone = responseBody["TelefoneComprador"] as? String,
            vehicleBrand = responseBody["MarcaVeiculo"] as? String,
            vehicleModel = responseBody["ModeloVeiculo"] as? String,
            vehicleYear = (responseBody["AnoVeiculo"] as? Number)?.toInt(),
            listingId = responseBody["CodigoAnuncio"] as? String,
            listingUrl = responseBody["UrlAnuncio"] as? String,
            contactType = resolveContactType(event.leadTypeId),
            receivedAt = event.receivedAt,
            rawPayload = rawJson
        )
    }

    /** Webmotors não possui endpoint de listagem paginada por data — exclusivamente webhook-driven */
    override fun fetchLeadsSince(credential: MarketplaceCredential, since: Instant): List<RawMarketplaceLead> = emptyList()

    private fun resolveContactType(leadTypeId: Int?): String? = when (leadTypeId) {
        1 -> "PhoneTracking"
        2 -> "Proposta"
        3 -> "PropostaZeroKm"
        4 -> "PropostaMoto"
        11 -> "WhatsappCTA"
        12 -> "WhatsAppCTAVeiculoInteresse"
        else -> leadTypeId?.toString()
    }
}
