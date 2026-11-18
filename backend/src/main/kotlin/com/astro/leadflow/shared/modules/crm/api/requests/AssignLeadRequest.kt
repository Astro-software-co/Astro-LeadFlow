package com.astro.leadflow.shared.modules.crm.api.requests

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AssignLeadRequest(
    @field:NotNull val userId: UUID
)