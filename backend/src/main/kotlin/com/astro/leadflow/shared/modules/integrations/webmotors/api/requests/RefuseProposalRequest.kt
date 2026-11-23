package com.astro.leadflow.shared.modules.integrations.webmotors.api.requests

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RefuseProposalRequest(
    @field:NotNull val reasonCode: Int?,
    @field:Size(max = 500) val message: String? = null
)
