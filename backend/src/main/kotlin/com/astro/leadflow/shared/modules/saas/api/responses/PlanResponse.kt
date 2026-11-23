package com.astro.leadflow.shared.modules.saas.api.responses

import com.astro.leadflow.shared.modules.saas.domain.BillingCycle
import java.math.BigDecimal
import java.util.UUID

data class PlanResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String,
    val priceBrl: BigDecimal,
    val billingCycle: BillingCycle,
    val billingCycleLabel: String,
    val messageLimit: Int,
    val marketplaceLimit: Int
)
