package com.astro.leadflow.shared.modules.crm.api.responses

import com.astro.leadflow.shared.modules.crm.domain.InterestType
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class LeadResponse(
    val id: UUID,
    val name: String?,
    val phoneNumber: String,
    val email: String?,
    val origin: String,
    val vehicleInterest: String?,
    val budget: BigDecimal?,
    val urgency: String?,
    val purchaseForecast: String?,
    val interestType: InterestType?,
    val notes: String?,
    val pipelineStage: PipelineStage,
    val assignedTo: SellerSummary?,
    val vehicle: VehicleSummary?,
    val isDuplicate: Boolean,
    val organizationId: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)






