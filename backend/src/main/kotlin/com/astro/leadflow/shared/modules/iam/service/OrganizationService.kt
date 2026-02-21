package com.astro.leadflow.shared.modules.iam.service

import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.DuplicateResourceException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateOrganizationRequest
import com.astro.leadflow.shared.modules.iam.api.requests.UpdateWhatsappCredentialsRequest
import com.astro.leadflow.shared.modules.iam.api.responses.OrganizationResponse
import com.astro.leadflow.shared.modules.iam.mapper.OrganizationMapper
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val organizationMapper: OrganizationMapper
) {

    @Transactional(readOnly = true)
    fun getMyOrganization(): OrganizationResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val org = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }
        return organizationMapper.toResponse(org)
    }

    @Transactional
    fun update(request: UpdateOrganizationRequest): OrganizationResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val org = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

        request.slug?.let { newSlug ->
            if (newSlug != org.slug && organizationRepository.existsBySlug(newSlug))
                throw DuplicateResourceException("Organization", "slug", newSlug)
            org.slug = newSlug
        }
        request.name?.let { org.name = it }

        return organizationMapper.toResponse(organizationRepository.save(org))
    }

    @Transactional
    fun updateWhatsappCredentials(
        request: UpdateWhatsappCredentialsRequest
    ): OrganizationResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val org = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

        org.whatsappToken = request.whatsappToken
        org.whatsappPhoneNumberId = request.whatsappPhoneNumberId

        return organizationMapper.toResponse(organizationRepository.save(org))
    }
}