package com.astro.leadflow.shared.modules.ingestion.api

import com.astro.leadflow.shared.modules.ingestion.api.requests.ScrapedLeadDTO
import com.astro.leadflow.shared.modules.ingestion.service.LeadIngestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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
        description = "Público mas protegido por API Key no API Gateway (AWS)"
    )
    @ApiResponse(responseCode = "202", description = "Lead aceito para processamento")
    fun receive(@Valid @RequestBody dto: ScrapedLeadDTO) {
        leadIngestionService.ingestFromWebhook(dto)
    }
}