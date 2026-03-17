package com.astro.leadflow.shared.modules.crm.mapper

import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadStageHistory
import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.api.responses.SellerSummary
import com.astro.leadflow.shared.modules.crm.api.responses.StageHistoryResponse
import com.astro.leadflow.shared.modules.crm.api.responses.VehicleSummary
import org.springframework.stereotype.Component

@Component
class LeadMapper {

    fun toResponse(lead: Lead): LeadResponse = LeadResponse(
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
        assignedTo = lead.assignedTo?.let { user ->
            SellerSummary(user.id, user.name, user.avatarUrl)
        },
        vehicle = lead.vehicle?.let { vehicle ->
            VehicleSummary(vehicle.id, vehicle.brand, vehicle.model, vehicle.year, vehicle.salePrice)
        },
        isDuplicate = lead.isDuplicate,
        organizationId = lead.organization.id,
        createdAt = lead.createdAt,
        updatedAt = lead.updatedAt
    )

    fun toHistory(h: LeadStageHistory): StageHistoryResponse = StageHistoryResponse(
        fromStage = h.fromStage,
        toStage = h.toStage,
        movedBy = h.movedBy?.let { user ->
            SellerSummary(user.id, user.name, user.avatarUrl)
        },
        movedAt = h.movedAt
    )
}