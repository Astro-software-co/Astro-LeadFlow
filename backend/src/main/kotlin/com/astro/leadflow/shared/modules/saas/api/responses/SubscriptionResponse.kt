package com.astro.leadflow.shared.modules.saas.api.responses

import com.astro.leadflow.shared.modules.saas.domain.BillingCycle
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SubscriptionResponse(
    val planId: UUID,
    val planName: String,
    val planDescription: String,
    val priceBrl: BigDecimal,
    val billingCycle: BillingCycle,
    val billingCycleLabel: String,
    val messageLimit: Int,
    val marketplaceLimit: Int,
    val status: SubscriptionStatus,
    val renewsAt: LocalDate?,
    val paymentProvider: String? = null,
    val checkoutUrl: String? = null,
    val canCancel: Boolean = false,
)
