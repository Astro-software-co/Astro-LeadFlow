package com.astro.leadflow.shared.modules.ingestion.api.requests

import jakarta.validation.constraints.NotBlank

data class ScrapedLeadWebhookRequest(
    @field:NotBlank val organizationSlug: String,
    @field:NotBlank val phoneNumber: String,
    val name: String? = null,
    val email: String? = null,
    @field:NotBlank val marketplace: String,
    val vehicleInterest: String? = null,
    val vehicleUrl: String? = null,
    val rawMessage: String? = null
)
