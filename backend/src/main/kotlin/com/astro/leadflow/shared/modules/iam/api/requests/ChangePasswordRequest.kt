package com.astro.leadflow.shared.modules.iam.api.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:Size(min = 8) val newPassword: String,
    @field:NotBlank val confirmNewPassword: String
)