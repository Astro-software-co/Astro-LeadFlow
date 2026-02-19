package com.astro.leadflow.modules.iam.api.requests

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class RegisterOrganizationRequest(
    @field:NotBlank val organizationName: String,
    @field:NotBlank val slug: String,
    @field:NotBlank val adminName: String,
    @field:Email @field:NotBlank val adminEmail: String,
    @field:Size(min = 8) val password: String
)

data class Verify2FARequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val code: String
)

data class RefreshTokenRequest(
    @field:NotBlank val refreshToken: String
)

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:Size(min = 8) val newPassword: String,
    @field:NotBlank val confirmNewPassword: String
)