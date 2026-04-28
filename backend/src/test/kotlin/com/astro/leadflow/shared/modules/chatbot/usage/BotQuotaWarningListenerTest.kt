package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.communication.email.EmailService
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class BotQuotaWarningListenerTest {
    @Test
    fun `sends warning to active astro admins`() {
        val userRepository = mockk<UserRepository>()
        val emailService = mockk<EmailService>(relaxed = true)
        val admin = User(name = "Admin", email = "admin@test.local", passwordHash = "hash", role = Role.ASTRO_ADMIN)
        every { userRepository.findAllByRoleAndStatus(Role.ASTRO_ADMIN, UserStatus.ACTIVE) } returns listOf(admin)

        BotQuotaWarningListener(userRepository, emailService)
            .onQuotaWarning(BotQuotaWarningEvent(UUID.randomUUID(), "Org", 80, 100))

        verify { emailService.sendBotQuotaWarning("admin@test.local", "Org", 80, 100) }
    }
}
