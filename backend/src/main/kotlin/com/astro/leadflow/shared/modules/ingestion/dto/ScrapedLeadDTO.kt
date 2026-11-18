package com.astro.leadflow.shared.modules.ingestion.dto

data class ScrapedLeadDTO(
    val organizationId: java.util.UUID,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val source: String,
    val marketplaceType: String,
    val rawData: Map<String, Any> = emptyMap(),
    val organizationSlug: String,
    val vehicleInterest: String
)
