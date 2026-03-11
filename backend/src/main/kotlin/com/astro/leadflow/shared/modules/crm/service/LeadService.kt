package com.astro.leadflow.shared.modules.crm.service

import com.astro.leadflow.modules.crm.api.requests.LeadCreateRequest
import com.astro.leadflow.modules.crm.api.requests.LeadUpdateRequest
import com.astro.leadflow.shared.modules.crm.api.responses.LeadResponse
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.crm.domain.LeadRepository
import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import com.astro.leadflow.shared.modules.crm.mapper.LeadMapper
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.inventory.domain.VehicleRepository
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LeadService(
    private val leadRepository: LeadRepository,
    private val organizationRepository: OrganizationRepository,
    private val vehicleRepository: VehicleRepository,
    private val leadMapper: LeadMapper
) {

    @Transactional
    fun create(request: LeadCreateRequest): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

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
                vehicle = vehicle as Vehicle?,
                isDuplicate = isDuplicate
            )
        )
        return leadMapper.toResponse(lead)
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
                .orElseThrow { ResourceNotFoundException("Vehicle", vehicleId) } as Vehicle?
        }

        return leadMapper.toResponse(leadRepository.save(lead))
    }

    @Transactional(readOnly = true)
    fun findById(leadId: UUID): LeadResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return leadMapper.toResponse(findLeadInOrg(leadId, orgId))
    }

    @Transactional(readOnly = true)
    fun findAll(): List<LeadResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return leadRepository.findAllByOrganizationId(orgId)
            .map { leadMapper.toResponse(it) }
    }

    @Transactional
    fun archive(leadId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = findLeadInOrg(leadId, orgId)
        if (lead.pipelineStage == PipelineStage.ARCHIVED)
            throw InvalidOperationException("Lead já está arquivado.")
        lead.pipelineStage = PipelineStage.ARCHIVED
        leadRepository.save(lead)
    }

    @Transactional
    fun delete(leadId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val lead = findLeadInOrg(leadId, orgId)
        leadRepository.delete(lead)
    }

    fun findLeadInOrg(leadId: UUID, orgId: UUID): Lead =
        leadRepository.findByOrganizationIdAndId(orgId, leadId)
            .orElseThrow { ResourceNotFoundException("Lead", leadId) }
}