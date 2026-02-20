package com.astro.leadflow.modules.crm.api.requests

import com.astro.leadflow.shared.modules.crm.domain.InterestType
import java.math.BigDecimal
import java.util.UUID

data class LeadUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val vehicleInterest: String? = null,
    val budget: BigDecimal? = null,
    val urgency: String? = null,
    val purchaseForecast: String? = null,
    val interestType: InterestType? = null,
    val notes: String? = null,
    val vehicleId: UUID? = null
)