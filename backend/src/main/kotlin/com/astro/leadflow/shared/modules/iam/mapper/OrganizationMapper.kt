package com.astro.leadflow.shared.modules.iam.mapper

import com.astro.leadflow.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.api.responses.OrganizationResponse
import org.springframework.stereotype.Component

@Component
class OrganizationMapper {

    fun toResponse(org: Organization): OrganizationResponse = OrganizationResponse(
        id = org.id,
        name = org.name,
        slug = org.slug,
        status = org.status,
        hasWhatsappConfigured = !org.whatsappToken.isNullOrBlank()
                && !org.whatsappPhoneNumberId.isNullOrBlank(),
        createdAt = org.createdAt
    )
}