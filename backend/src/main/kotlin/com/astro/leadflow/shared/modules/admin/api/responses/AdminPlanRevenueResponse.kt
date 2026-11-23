package com.astro.leadflow.shared.modules.admin.api.responses

import java.math.BigDecimal

data class AdminPlanRevenueResponse(
    val planName: String,
    val mrrBrl: BigDecimal,
    val subscriptions: Long
)
