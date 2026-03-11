package com.astro.leadflow.shared.modules.iam.api.responses

import com.astro.leadflow.shared.modules.iam.domain.OrganizationStatus
import java.time.LocalDateTime
import java.util.UUID

data class OrganizationResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val status: OrganizationStatus,
    val hasWhatsappConfigured: Boolean,
    val createdAt: LocalDateTime
)