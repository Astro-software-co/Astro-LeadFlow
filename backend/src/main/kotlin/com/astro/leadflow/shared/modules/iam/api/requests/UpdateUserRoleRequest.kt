package com.astro.leadflow.shared.modules.iam.api.requests

import com.astro.leadflow.shared.modules.iam.domain.Role
import jakarta.validation.constraints.NotNull

data class UpdateUserRoleRequest(
    @field:NotNull val role: Role
)