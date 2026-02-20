package com.astro.leadflow.modules.crm.api.responses

import com.astro.leadflow.modules.crm.domain.InterestType
import com.astro.leadflow.modules.crm.domain.PipelineStage
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

data class KanbanBoardResponse(
    val stages: Map<PipelineStage, List<LeadResponse>>
)

data class SellerSummary(
    val id: UUID,
    val name: String,
    val avatarUrl: String?
)

data class VehicleSummary(
    val id: UUID,
    val brand: String,
    val model: String,
    val year: Int,
    val salePrice: BigDecimal
)

data class StageHistoryResponse(
    val fromStage: PipelineStage?,
    val toStage: PipelineStage,
    val movedBy: SellerSummary?,
    val movedAt: LocalDateTime
)