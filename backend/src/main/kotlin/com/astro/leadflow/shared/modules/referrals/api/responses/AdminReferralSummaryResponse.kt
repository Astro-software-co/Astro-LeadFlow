package com.astro.leadflow.shared.modules.referrals.api.responses

import java.math.BigDecimal

data class AdminReferralSummaryResponse(
    val totalReferrers: Long,
    val totalReferrals: Long,
    val totalCommissionPaidBrl: BigDecimal,
    val pendingCommissionBrl: BigDecimal,
    val topReferrers: List<TopReferrerResponse>
)
