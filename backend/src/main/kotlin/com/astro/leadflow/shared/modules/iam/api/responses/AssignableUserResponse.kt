package com.astro.leadflow.shared.modules.iam.api.responses

import com.astro.leadflow.shared.modules.iam.domain.Role
import java.util.UUID

data class AssignableUserResponse(
    val id: UUID,
    val name: String,
    val avatarUrl: String?,
    val role: Role,
)
