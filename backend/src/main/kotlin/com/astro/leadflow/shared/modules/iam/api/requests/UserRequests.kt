package com.astro.leadflow.modules.iam.api.requests

import com.astro.leadflow.modules.iam.domain.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:Email @field:NotBlank val email: String,
    @field:NotNull val role: Role,
    val phoneNumber: String? = null
)

data class UpdateUserRequest(
    val name: String? = null,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null
)

data class UpdateUserRoleRequest(
    @field:NotNull val role: Role
)

data class UpdateNotificationsRequest(
    val notifyNewLead: Boolean? = null,
    val notifyLeadWaiting: Boolean? = null,
    val notifySaleCompleted: Boolean? = null,
    val notifyCardMoved: Boolean? = null,
    val notifyViaEmail: Boolean? = null,
    val notifyViaPush: Boolean? = null
)

data class UpdateTwoFactorRequest(
    val emailEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null
)