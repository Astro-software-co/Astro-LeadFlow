package com.astro.leadflow.shared.modules.iam.api.requests

import com.astro.leadflow.shared.modules.iam.domain.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:Email @field:NotBlank val email: String,
    @field:NotNull val role: Role,
    val phoneNumber: String? = null
)