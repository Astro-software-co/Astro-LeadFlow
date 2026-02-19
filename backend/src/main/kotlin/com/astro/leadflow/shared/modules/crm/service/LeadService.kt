package com.astro.leadflow.modules.crm.service

import com.astro.leadflow.modules.crm.api.requests.LeadCreateRequest
import com.astro.leadflow.modules.crm.api.requests.LeadUpdateRequest
import com.astro.leadflow.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.modules.crm.domain.Lead
import com.astro.leadflow.modules.crm.domain.LeadRepository
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.modules.inventory.domain.VehicleRepository
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LeadService(
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val vehicleRepository: VehicleRepository
) {

    @Transactional
    fun create(request: LeadCreateRequest): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

        // Checa duplicidade por telefone
        val isDuplicate = leadRepository.existsByOrganizationIdAndPhoneNumber(
            orgId, request.phoneNumber
        )

        val vehicle = request.vehicleId?.let {
            vehicleRepository.findByOrganizationIdAndId(orgId, it)
                .orElseThrow { ResourceNotFoundException("Vehicle", it) }
        }

        val lead = leadRepository.save(
            Lead(
                organization = organization,
                phoneNumber = request.phoneNumber,
                name = request.name,
                email = request.email,
                origin = request.origin,
                vehicleInterest = request.vehicleInterest,
                budget = request.budget,
                urgency = request.urgency,
                purchaseForecast = request.purchaseForecast,
                interestType = request.interestType,
                notes = request.notes,
                vehicle = vehicle,
                isDuplicate = isDuplicate
            )
        )

        return LeadResponse.from(lead)
    }

    @Transactional
    fun update(leadId: UUID, request: LeadUpdateRequest): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = findLeadInOrg(leadId, orgId)

        request.name?.let { lead.name = it }
        request.email?.let { lead.email = it }
        request.vehicleInterest?.let { lead.vehicleInterest = it }
        request.budget?.let { lead.budget = it }
        request.urgency?.let { lead.urgency = it }
        request.purchaseForecast?.let { lead.purchaseForecast = it }
        request.interestType?.let { lead.interestType = it }
        request.notes?.let { lead.notes = it }

        request.vehicleId?.let { vehicleId ->
            lead.vehicle = vehicleRepository.findByOrganizationIdAndId(orgId, vehicleId)
                .orElseThrow { ResourceNotFoundException("Vehicle", vehicleId) }
        }

        return LeadResponse.from(leadRepository.save(lead))
    }

    @Transactional(readOnly = true)
    fun findById(leadId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return LeadResponse.from(findLeadInOrg(leadId, orgId))
    }

    @Transactional(readOnly = true)
    fun findAll(): List<LeadResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return leadRepository.findAllByOrganizationId(orgId).map { LeadResponse.from(it) }
    }

    @Transactional
    fun archive(leadId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = findLeadInOrg(leadId, orgId)
        lead.pipelineStage = com.astro.leadflow.modules.crm.domain.PipelineStage.ARCHIVED
        leadRepository.save(lead)
    }

    // Helper interno — garante multi-tenancy em todas as operações
    fun findLeadInOrg(leadId: UUID, orgId: UUID): Lead =
        leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }
}