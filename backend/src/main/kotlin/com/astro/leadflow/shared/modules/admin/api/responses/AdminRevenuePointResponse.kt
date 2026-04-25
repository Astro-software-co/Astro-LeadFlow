package com.astro.leadflow.shared.modules.admin.api.responses

import java.math.BigDecimal

data class AdminRevenuePointResponse(
    val month: String,
    val revenueBrl: BigDecimal
)
