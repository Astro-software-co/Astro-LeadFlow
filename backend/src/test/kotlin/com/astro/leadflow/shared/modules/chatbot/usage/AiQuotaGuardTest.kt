package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class AiQuotaGuardTest {
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val usageRepository = mockk<AiTokenUsageRepository>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val guard = AiQuotaGuard(subscriptionRepository, usageRepository, publisher)
    private val organization = Organization(name = "Astro", slug = "astro")

    @Test
    fun `allows unlimited plans`() {
        every { subscriptionRepository.findByOrganizationId(organization.id) } returns Optional.of(subscription(null))

        assertTrue(guard.canUseTokens(organization.id, 100))
    }

    @Test
    fun `blocks projected usage above limit`() {
        every { subscriptionRepository.findByOrganizationId(organization.id) } returns Optional.of(subscription(100))
        every { usageRepository.sumTokensByOrganizationAndPeriod(eq(organization.id), any(), any()) } returns 100

        assertFalse(guard.canUseTokens(organization.id, 1))
    }

    @Test
    fun `publishes warning at eighty percent`() {
        every { subscriptionRepository.findByOrganizationId(organization.id) } returns Optional.of(subscription(100))
        every { usageRepository.sumTokensByOrganizationAndPeriod(eq(organization.id), any(), any()) } returns 79

        assertTrue(guard.canUseTokens(organization.id, 1))
        verify { publisher.publishEvent(match<BotQuotaWarningEvent> { it.usedTokens == 80 }) }
    }

    private fun subscription(maxTokens: Int?): Subscription =
        Subscription(
            organization = organization,
            plan = Plan(
                name = "Enterprise",
                priceBrl = BigDecimal.TEN,
                messageLimit = 100,
                marketplaceLimit = 1,
                maxAiTokensPerMonth = maxTokens
            )
        )
}
