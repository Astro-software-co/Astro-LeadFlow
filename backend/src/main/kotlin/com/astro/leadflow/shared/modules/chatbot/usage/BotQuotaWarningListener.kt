package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.communication.email.EmailService
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BotQuotaWarningListener(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    @EventListener
    fun onQuotaWarning(event: BotQuotaWarningEvent) {
        userRepository.findAllByRoleAndStatus(Role.ASTRO_ADMIN, UserStatus.ACTIVE)
            .forEach { admin ->
                emailService.sendBotQuotaWarning(
                    to = admin.email,
                    organizationName = event.organizationName,
                    usedTokens = event.usedTokens,
                    maxTokens = event.maxTokens
                )
            }
    }
}
