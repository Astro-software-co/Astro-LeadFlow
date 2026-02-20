package com.astro.leadflow.shared.modules.crm.api.responses

import java.math.BigDecimal
import java.util.UUID

data class VehicleSummary(
    val id: UUID,
    val brand: String,
    val model: String,
    val year: Int,
    val salePrice: BigDecimal
)