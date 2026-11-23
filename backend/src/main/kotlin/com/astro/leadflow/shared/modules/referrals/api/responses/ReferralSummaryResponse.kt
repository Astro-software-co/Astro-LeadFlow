package com.astro.leadflow.shared.modules.referrals.api.responses

import java.math.BigDecimal

data class ReferralSummaryResponse(
    val code: String,
    val referralLink: String,
    val approvedCount: Long,
    val pendingCount: Long,
    val currentTier: String,
    val commissionRate: BigDecimal,
    val nextTierThreshold: Int,
    val balanceBrl: BigDecimal,
    val referrals: List<ReferredCompanyResponse>
)
