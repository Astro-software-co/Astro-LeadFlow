package com.astro.leadflow.shared.modules.crm.api.requests

import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import jakarta.validation.constraints.NotNull

data class MoveLeadRequest(
    @field:NotNull val toStage: PipelineStage
)