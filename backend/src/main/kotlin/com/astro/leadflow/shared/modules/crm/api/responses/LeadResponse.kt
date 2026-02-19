package com.astro.leadflow.modules.crm.api.responses

import com.astro.leadflow.modules.crm.domain.InterestType
import com.astro.leadflow.modules.crm.domain.Lead
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(lead: Lead) = LeadResponse(
            id = lead.id,
            name = lead.name,
            phoneNumber = lead.phoneNumber,
            email = lead.email,
            origin = lead.origin,
            vehicleInterest = lead.vehicleInterest,
            budget = lead.budget,
            urgency = lead.urgency,
            purchaseForecast = lead.purchaseForecast,
            interestType = lead.interestType,
            notes = lead.notes,
            pipelineStage = lead.pipelineStage,
            assignedTo = lead.assignedTo?.let {
                SellerSummary(it.id, it.name)
            },
            vehicle = lead.vehicle?.let {
                VehicleSummary(it.id, it.brand, it.model, it.year)
            },
            isDuplicate = lead.isDuplicate,
            createdAt = lead.createdAt,
            updatedAt = lead.updatedAt
        )
    }
}

data class SellerSummary(val id: UUID, val name: String)
data class VehicleSummary(val id: UUID, val brand: String, val model: String, val year: Int)

