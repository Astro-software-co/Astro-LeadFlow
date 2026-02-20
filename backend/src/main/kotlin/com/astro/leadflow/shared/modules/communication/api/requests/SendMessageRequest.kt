package com.astro.leadflow.shared.modules.communication.api.requests

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class SendMessageRequest(
    @field:NotBlank val phoneNumber: String,
    @field:NotBlank val message: String,
    val leadId: UUID? = null
)