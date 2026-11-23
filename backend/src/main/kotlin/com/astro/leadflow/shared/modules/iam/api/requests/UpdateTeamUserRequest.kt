package com.astro.leadflow.shared.modules.iam.api.requests

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateTeamUserRequest(
    @field:NotBlank val name: String,
    @field:Email @field:NotBlank val email: String,
    val phoneNumber: String? = null
)
