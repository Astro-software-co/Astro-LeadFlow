package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfig
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceConfigRepository
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import com.astro.leadflow.shared.modules.saas.domain.UsageLog
import com.astro.leadflow.shared.modules.saas.domain.UsageLogRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class UsageLimitServiceTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var marketplaceConfigRepository: MarketplaceConfigRepository
    private lateinit var usageLogRepository: UsageLogRepository
    private lateinit var usageLimitService: UsageLimitService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(id = orgId, name = "Test", slug = "test")
    private val plan = Plan(
        name = "Básico",
        monthlyPriceBrl = BigDecimal("149.00"),
        messageLimit = 1000,
        marketplaceLimit = 1
    )
    private val subscription = Subscription(
        organization = organization,
        plan = plan,
        status = SubscriptionStatus.ACTIVE
    )

    @BeforeEach
    fun setup() {
        subscriptionRepository = mockk()
        marketplaceConfigRepository = mockk()
        usageLogRepository = mockk()
        usageLimitService = UsageLimitService(
            subscriptionRepository,
            marketplaceConfigRepository,
            usageLogRepository
        )
    }

    @Test
    fun `canSendMessage should return true when under global limit`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns
                Optional.of(subscription)
        every {
            usageLogRepository.sumMessagesByOrganizationAndPeriod(orgId, any(), any())
        } returns 500L

        Assertions.assertTrue(usageLimitService.canSendMessage(orgId))
    }

    @Test
    fun `canSendMessage should return false when global limit is reached`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns
                Optional.of(subscription)
        every {
            usageLogRepository.sumMessagesByOrganizationAndPeriod(orgId, any(), any())
        } returns 1000L

        Assertions.assertFalse(usageLimitService.canSendMessage(orgId))
    }

    @Test
    fun `canSendMessage should return false when subscription is not active`() {
        val suspended = Subscription(
            organization = organization,
            plan = plan,
            status = SubscriptionStatus.SUSPENDED
        )
        every { subscriptionRepository.findByOrganizationId(orgId) } returns
                Optional.of(suspended)

        Assertions.assertFalse(usageLimitService.canSendMessage(orgId))
    }

    @Test
    fun `canSendMessage should return false when no subscription found`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.empty()

        Assertions.assertFalse(usageLimitService.canSendMessage(orgId))
    }

    @Test
    fun `canSendMessage should return false when marketplace limit is reached`() {
        val marketplaceConfig = MarketplaceConfig(
            organization = organization,
            marketplaceName = "WEBMOTORS",
            enabled = true,
            monthlyLimit = 200
        )

        every { subscriptionRepository.findByOrganizationId(orgId) } returns
                Optional.of(subscription)
        every {
            usageLogRepository.sumMessagesByOrganizationAndPeriod(orgId, any(), any())
        } returns 100L
        every {
            marketplaceConfigRepository.findByOrganizationIdAndMarketplaceName(
                orgId, "WEBMOTORS"
            )
        } returns Optional.of(marketplaceConfig)
        every {
            usageLogRepository.findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
                orgId, "WEBMOTORS", any(), any()
            )
        } returns Optional.of(
            UsageLog(
                organization = organization,
                marketplaceName = "WEBMOTORS",
                messagesSent = 200,
                periodMonth = 1,
                periodYear = 2026
            )
        )

        Assertions.assertFalse(usageLimitService.canSendMessage(orgId, "WEBMOTORS"))
    }

    @Test
    fun `canSendMessage should return false when marketplace is disabled`() {
        val disabledConfig = MarketplaceConfig(
            organization = organization,
            marketplaceName = "OLX",
            enabled = false,
            monthlyLimit = 200
        )

        every { subscriptionRepository.findByOrganizationId(orgId) } returns
                Optional.of(subscription)
        every {
            usageLogRepository.sumMessagesByOrganizationAndPeriod(orgId, any(), any())
        } returns 0L
        every {
            marketplaceConfigRepository.findByOrganizationIdAndMarketplaceName(orgId, "OLX")
        } returns Optional.of(disabledConfig)

        Assertions.assertFalse(usageLimitService.canSendMessage(orgId, "OLX"))
    }
}