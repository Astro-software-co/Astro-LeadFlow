package com.astro.leadflow.shared.modules.iam.api.requests

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterOrganizationRequest(
    @field:NotBlank val organizationName: String,
    @field:NotBlank val slug: String,
    @field:NotBlank val adminName: String,
    @field:Email @field:NotBlank val adminEmail: String,
    @field:Size(min = 8) val password: String
)