package com.astro.leadflow.shared.modules.iam.api.requests

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class Verify2FARequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val code: String
)