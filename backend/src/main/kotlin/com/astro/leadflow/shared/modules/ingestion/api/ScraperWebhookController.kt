package com.astro.leadflow.shared.modules.ingestion.api

import com.astro.leadflow.shared.modules.ingestion.api.requests.ScrapedLeadDTO
import com.astro.leadflow.modules.ingestion.service.LeadIngestionService
import jakarta.validation.Valid
import org.junit.jupiter.api.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhooks/scraper")
@Tag(name = "Ingestion — Scrapers", description = "Entrada de leads capturados pelos robôs")
class ScraperWebhookController(
    private val leadIngestionService: LeadIngestionService
) {

    /**
     * Endpoint chamado pelas Lambdas de scraping.
     * Público mas protegido por API Key no API Gateway (AWS).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Recebe lead capturado por scraper externo")
    fun receive(@Valid @RequestBody dto: ScrapedLeadDTO) {
        leadIngestionService.ingestFromWebhook(dto)
    }
}