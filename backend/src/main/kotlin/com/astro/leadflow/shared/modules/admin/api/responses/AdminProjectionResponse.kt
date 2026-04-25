package com.astro.leadflow.shared.modules.admin.api.responses

import java.math.BigDecimal

data class AdminProjectionResponse(
    val months: Int,
    val projectedMrrBrl: BigDecimal,
    val averageMonthlyGrowthBrl: BigDecimal
)
