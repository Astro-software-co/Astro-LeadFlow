package com.astro.leadflow.modules.crm.api.requests

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AssignLeadRequest(
    @field:NotNull val sellerId: UUID
)