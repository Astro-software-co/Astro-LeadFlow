package com.astro.leadflow.shared.modules.saas.api.responses

import java.math.BigDecimal
import java.util.UUID

data class PlanResponse(
    val id: UUID,
    val name: String,
    val monthlyPriceBrl: BigDecimal,
    val messageLimit: Int,
    val marketplaceLimit: Int
)