package com.astro.leadflow.shared.modules.monitoring.controller.dto

import java.util.UUID

data class OrganizationTokenUsageResponse(
    val organizationId: UUID,
    val tokens: Long
)
