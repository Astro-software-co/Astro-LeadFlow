package com.astro.leadflow.shared.modules.iam.api.responses

import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val avatarUrl: String?,
    val role: Role,
    val status: UserStatus,
    val organizationId: UUID?,
    val organizationName: String?,
    val twoFactorEmailEnabled: Boolean,
    val twoFactorSmsEnabled: Boolean,
    val notifyNewLead: Boolean,
    val notifyLeadWaiting: Boolean,
    val notifySaleCompleted: Boolean,
    val notifyCardMoved: Boolean,
    val notifyViaEmail: Boolean,
    val notifyViaPush: Boolean,
    val createdAt: LocalDateTime
)