package com.astro.leadflow.shared.modules.admin.api.responses

import java.math.BigDecimal
import java.time.LocalDate

data class AdminSubscriptionRowResponse(
    val empresa: String,
    val plano: String,
    val status: String,
    val mrr: BigDecimal,
    val proximaCobranca: LocalDate?,
    val origem: String,
    val inicio: LocalDate,
    val cancelamento: LocalDate?,
    val motivoCancelamento: String?
)
