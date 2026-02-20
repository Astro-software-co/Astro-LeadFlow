package com.astro.leadflow.modules.crm.api.requests

import com.astro.leadflow.modules.crm.domain.InterestType
import com.astro.leadflow.modules.crm.domain.PipelineStage
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class LeadCreateRequest(
    val name: String? = null,
    @field:NotBlank val phoneNumber: String,
    val email: String? = null,
    @field:NotBlank val origin: String,
    val vehicleInterest: String? = null,
    val budget: BigDecimal? = null,
    val urgency: String? = null,
    val purchaseForecast: String? = null,
    val interestType: InterestType? = null,
    val notes: String? = null,
    val vehicleId: UUID? = null
)





