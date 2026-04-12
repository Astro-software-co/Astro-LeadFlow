package com.astro.leadflow.shared.modules.crm.api.responses

import com.astro.leadflow.shared.modules.iam.domain.Role
import java.util.UUID

data class UserSummaryResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: Role
)