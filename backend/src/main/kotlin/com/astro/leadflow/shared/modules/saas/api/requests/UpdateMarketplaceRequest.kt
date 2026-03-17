package com.astro.leadflow.shared.modules.saas.api.requests

import jakarta.validation.constraints.Min

data class UpdateMarketplaceRequest(
    val enabled: Boolean? = null,
    @field:Min(1) val monthlyLimit: Int? = null
)
