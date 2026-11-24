package com.astro.leadflow.shared.modules.privacy.api.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RejectDataSubjectRequestRequest(
    @field:NotBlank @field:Size(max = 2000) val reason: String?
)
