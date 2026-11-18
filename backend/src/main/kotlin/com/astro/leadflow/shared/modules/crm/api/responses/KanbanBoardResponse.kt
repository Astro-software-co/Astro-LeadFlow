package com.astro.leadflow.shared.modules.crm.api.responses

import com.astro.leadflow.shared.modules.crm.domain.PipelineStage

data class KanbanBoardResponse(
    val stages: Map<PipelineStage, List<LeadResponse>>
)