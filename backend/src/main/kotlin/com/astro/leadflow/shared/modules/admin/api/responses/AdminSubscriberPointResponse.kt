package com.astro.leadflow.shared.modules.admin.api.responses

import java.math.BigDecimal

data class AdminSubscriberPointResponse(
    val month: String,
    val activeSubscribers: Long,
    val projectedMrr: BigDecimal
)
