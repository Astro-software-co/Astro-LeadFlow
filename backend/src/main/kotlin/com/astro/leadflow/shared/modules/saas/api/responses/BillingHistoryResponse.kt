package com.astro.leadflow.shared.modules.saas.api.responses

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class BillingHistoryResponse(
    val id: UUID,
    val billedAt: LocalDate,
    val issuedAt: LocalDate,
    val description: String,
    val amountBrl: BigDecimal,
    val status: String,
    val invoiceUrl: String? = null,
)
