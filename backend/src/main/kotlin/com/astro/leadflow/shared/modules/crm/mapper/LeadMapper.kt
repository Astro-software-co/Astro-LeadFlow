package com.astro.leadflow.modules.crm.mapper

import com.astro.leadflow.modules.crm.api.responses.*
import com.astro.leadflow.modules.crm.domain.Lead
import com.astro.leadflow.modules.crm.domain.LeadStageHistory
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
        assignedTo = lead.assignedTo?.let {
            SellerSummary(it.id, it.name, it.avatarUrl)
        },
        vehicle = lead.vehicle?.let {
            VehicleSummary(it.id, it.brand, it.model, it.year, it.salePrice)
        },
        isDuplicate = lead.isDuplicate,
        organizationId = lead.organization.id,
        createdAt = lead.createdAt,
        updatedAt = lead.updatedAt
    )

    fun toHistory(h: LeadStageHistory): StageHistoryResponse = StageHistoryResponse(
        fromStage = h.fromStage,
        toStage = h.toStage,
        movedBy = h.movedBy?.let {
            SellerSummary(it.id, it.name, it.avatarUrl)
        },
        movedAt = h.movedAt
    )
}