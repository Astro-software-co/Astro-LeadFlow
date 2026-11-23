package com.astro.leadflow.shared.modules.referrals.api.responses

import java.math.BigDecimal

data class TopReferrerResponse(
    val companyName: String,
    val referralCode: String,
    val approvedCount: Long,
    val tier: String,
    val totalCommissionBrl: BigDecimal
)
