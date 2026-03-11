package com.astro.leadflow.shared.modules.saas.api.responses

import java.util.UUID

data class MarketplaceConfigResponse(
    val id: UUID,
    val marketplaceName: String,
    val enabled: Boolean,
    val monthlyLimit: Int
)