package com.astro.leadflow.shared.modules.iam.mapper

import com.astro.leadflow.shared.modules.iam.api.responses.UserResponse
import com.astro.leadflow.shared.modules.iam.api.responses.AssignableUserResponse
import com.astro.leadflow.shared.modules.iam.api.responses.UserSummaryResponse
import com.astro.leadflow.shared.modules.iam.domain.User
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toResponse(user: User): UserResponse = UserResponse(
        id = user.id,
        name = user.name,
        email = user.email,
        phoneNumber = user.phoneNumber,
        avatarUrl = user.avatarUrl,
        role = user.role,
        referralCode = user.referralCode,
        status = user.status,
        organizationId = user.organization?.id,
        organizationName = user.organization?.name,
        twoFactorEmailEnabled = user.twoFactorEmailEnabled,
        twoFactorSmsEnabled = user.twoFactorSmsEnabled,
        notifyNewLead = user.notifyNewLead,
        notifyLeadWaiting = user.notifyLeadWaiting,
        notifySaleCompleted = user.notifySaleCompleted,
        notifyCardMoved = user.notifyCardMoved,
        notifyViaEmail = user.notifyViaEmail,
        notifyViaPush = user.notifyViaPush,
        createdAt = user.createdAt
    )

    fun toSummary(user: User): UserSummaryResponse = UserSummaryResponse(
        id = user.id,
        name = user.name,
        email = user.email,
        avatarUrl = user.avatarUrl,
        role = user.role,
        status = user.status
    )

    fun toAssignable(user: User): AssignableUserResponse = AssignableUserResponse(
        id = user.id,
        name = user.name,
        avatarUrl = user.avatarUrl,
        role = user.role,
    )
}
