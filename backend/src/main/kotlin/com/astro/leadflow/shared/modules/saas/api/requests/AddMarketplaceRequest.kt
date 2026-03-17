package com.astro.leadflow.shared.modules.saas.api.requests

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AddMarketplaceRequest(
    @field:NotBlank val marketplaceName: String,
    @field:NotNull @field:Min(1) val monthlyLimit: Int
)
