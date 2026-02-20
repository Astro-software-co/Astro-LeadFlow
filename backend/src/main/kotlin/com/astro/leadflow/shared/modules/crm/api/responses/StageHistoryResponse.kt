package com.astro.leadflow.shared.modules.crm.api.responses

import com.astro.leadflow.shared.modules.crm.domain.PipelineStage
import java.time.LocalDateTime

data class StageHistoryResponse(
    val fromStage: PipelineStage?,
    val toStage: PipelineStage,
    val movedBy: SellerSummary?,
    val movedAt: LocalDateTime
)