package com.astro.leadflow.modules.saas.api.responses

import com.astro.leadflow.modules.saas.domain.SubscriptionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SubscriptionResponse(
    val planId: UUID,
    val planName: String,
    val monthlyPriceBrl: BigDecimal,
    val messageLimit: Int,
    val marketplaceLimit: Int,
    val status: SubscriptionStatus,
    val renewsAt: LocalDate?
)

data class MarketplaceConfigResponse(
    val id: UUID,
    val marketplaceName: String,
    val enabled: Boolean,
    val monthlyLimit: Int
)

data class PlanResponse(
    val id: UUID,
    val name: String,
    val monthlyPriceBrl: BigDecimal,
    val messageLimit: Int,
    val marketplaceLimit: Int
)

data class UsageSummaryResponse(
    val planName: String,
    val globalLimit: Long,
    val globalUsed: Long,
    val globalRemaining: Long,
    val renewsAt: LocalDate?,
    val marketplaces: List<MarketplaceUsageDetail>
)

data class MarketplaceUsageDetail(
    val marketplaceName: String,
    val enabled: Boolean,
    val used: Long,
    val limit: Long,
    val remaining: Long
)