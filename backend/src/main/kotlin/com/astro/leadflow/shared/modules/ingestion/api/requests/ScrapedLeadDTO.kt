package com.astro.leadflow.shared.modules.ingestion.api.requests

import jakarta.validation.constraints.NotBlank

data class ScrapedLeadDTO(
    // Identifica qual organização/concessionária está recebendo o lead
    @field:NotBlank val organizationSlug: String,

    @field:NotBlank val phoneNumber: String,
    val name: String? = null,
    val email: String? = null,

    // Marketplace de origem: WEBMOTORS, OLX, MERCADO_LIVRE, etc.
    @field:NotBlank val marketplace: String,

    val vehicleInterest: String? = null,
    val vehicleUrl: String? = null,   // URL do anúncio no marketplace
    val rawMessage: String? = null    // Mensagem original do interessado
)