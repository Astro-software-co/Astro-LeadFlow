package com.astro.leadflow.shared.modules.referrals.api.responses

import java.math.BigDecimal

data class ReferralBalanceResponse(
    val balanceBrl: BigDecimal,
    val pendingBrl: BigDecimal,
    val withdrawalAvailable: Boolean
)
