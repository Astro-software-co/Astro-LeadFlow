package com.astro.leadflow.shared.modules.ingestion.api

import com.astro.leadflow.shared.config.ApiKeyAuthFilter
import com.astro.leadflow.shared.modules.ingestion.api.requests.ScrapedLeadWebhookRequest
import com.astro.leadflow.shared.modules.ingestion.dto.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/webhooks/scraper")
@Tag(name = "Ingestion — Scrapers", description = "Entrada de leads capturados pelos robôs")
class ScraperWebhookController(
    private val leadIngestionService: LeadIngestionService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Recebe lead capturado por scraper externo",
        description = "Protegido por API Key (header X-Api-Key). A organização é identificada automaticamente pela chave."
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Lead aceito para processamento"),
        ApiResponse(responseCode = "401", description = "API Key ausente ou inválida")
    )
    fun receive(
        @Valid @RequestBody request: ScrapedLeadWebhookRequest,
        @Parameter(hidden = true) httpRequest: HttpServletRequest
    ) {
        val organizationId = httpRequest.getAttribute(ApiKeyAuthFilter.ORG_ID_ATTRIBUTE) as UUID
        leadIngestionService.ingestFromWebhook(
            ScrapedLeadDTO(
                organizationId = organizationId,
                organizationSlug = request.organizationSlug,
                name = request.name ?: "",
                phoneNumber = request.phoneNumber,
                email = request.email,
                source = "SCRAPER",
                marketplaceType = request.marketplace,
                vehicleInterest = request.vehicleInterest ?: ""
            )
        )
    }
}