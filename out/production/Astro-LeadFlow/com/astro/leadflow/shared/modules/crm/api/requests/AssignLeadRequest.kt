package com.astro.leadflow.modules.crm.api.requests

import com.astro.leadflow.modules.crm.domain.InterestType
import com.astro.leadflow.modules.crm.domain.PipelineStage
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class AssignLeadRequest(
    @field:NotNull val sellerId: UUID
)