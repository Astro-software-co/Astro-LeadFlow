package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.saas.domain.*
import com.astro.leadflow.shared.modules.saas.mapper.SaasMapper
import com.astro.leadflow.shared.util.SecurityUtils
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class SubscriptionServiceTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var planRepository: PlanRepository
    private lateinit var marketplaceConfigRepository: MarketplaceConfigRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var billingHistoryEntryRepository: BillingHistoryEntryRepository
    private lateinit var saasMapper: SaasMapper
    private lateinit var usageLogRepository: UsageLogRepository
    private lateinit var service: SubscriptionService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(id = orgId, name = "Test Org", slug = "test-org")
    private val plan = Plan(
        name = "Básico",
        priceBrl = BigDecimal("149.00"),
        messageLimit = 1000,
        marketplaceLimit = 2
    )
    private val subscription = Subscription(
        organization = organization,
        plan = plan,
        status = SubscriptionStatus.ACTIVE
    )

    @BeforeEach
    fun setup() {
        subscriptionRepository = mockk()
        planRepository = mockk()
        marketplaceConfigRepository = mockk()
        organizationRepository = mockk()
        billingHistoryEntryRepository = mockk()
        saasMapper = SaasMapper()
        usageLogRepository = mockk()
        
        service = SubscriptionService(
            subscriptionRepository,
            planRepository,
            marketplaceConfigRepository,
            organizationRepository,
            billingHistoryEntryRepository,
            usageLogRepository,
            saasMapper
        )

        mockkObject(SecurityUtils)
        val user = mockk<User>()
        every { user.organization } returns organization
        every { SecurityUtils.getCurrentUser() } returns user
        every { SecurityUtils.getCurrentOrganizationId() } returns orgId
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SecurityUtils)
    }

    // ── getMySubscription ──────────────────────────────────────────────

    @Test
    fun `should return subscription response for current user`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)

        val response = service.getMySubscription()

        assertEquals(plan.name, response.planName)
        assertEquals(plan.messageLimit, response.messageLimit)
    }

    @Test
    fun `should throw ResourceNotFoundException when no subscription found`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.getMySubscription()
        }
    }

    // ── listBillingHistory ──────────────────────────────────────────────

    @Test
    fun `should return billing history when data exists`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every {
            billingHistoryEntryRepository.findAllBySubscriptionIdOrderByBilledAtDescCreatedAtDesc(subscription.id)
        } returns emptyList()
        
        val usage = UsageLog(
            organization = organization,
            marketplaceName = null,
            periodMonth = 1,
            periodYear = 2024,
            createdAt = LocalDateTime.of(2024, 1, 15, 10, 0)
        )
        
        every { 
            usageLogRepository.findAllByOrganizationIdAndMarketplaceNameIsNullOrderByPeriodYearDescPeriodMonthDescCreatedAtDesc(orgId)
        } returns listOf(usage)

        val history = service.listBillingHistory()

        assertEquals(1, history.size)
        val item = history[0]
        assertEquals(usage.id, item.id)
        assertTrue(item.description!!.contains("Básico"))
        assertTrue(item.description!!.contains("01/2024"))
        assertEquals("PAID", item.status)
        assertEquals(plan.priceBrl, item.amountBrl)
    }

    @Test
    fun `should return empty billing history when no usage logs found`() {
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every {
            billingHistoryEntryRepository.findAllBySubscriptionIdOrderByBilledAtDescCreatedAtDesc(subscription.id)
        } returns emptyList()
        every { 
            usageLogRepository.findAllByOrganizationIdAndMarketplaceNameIsNullOrderByPeriodYearDescPeriodMonthDescCreatedAtDesc(orgId)
        } returns emptyList()

        val history = service.listBillingHistory()

        assertTrue(history.isEmpty())
    }

    // ── changePlan ──────────────────────────────────────────────────────

    @Test
    fun `should change plan successfully`() {
        val newPlanId = UUID.randomUUID()
        val newPlan = Plan(
            id = newPlanId,
            name = "Pro",
            priceBrl = BigDecimal("299.00"),
            messageLimit = 5000,
            marketplaceLimit = 5
        )

        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every { planRepository.findById(newPlanId) } returns Optional.of(newPlan)
        every { subscriptionRepository.save(any()) } returns subscription

        val response = service.changePlan(newPlanId)

        assertEquals("Pro", subscription.plan.name)
        verify { subscriptionRepository.save(subscription) }
    }

    @Test
    fun `should throw when target plan not found`() {
        val unknownPlanId = UUID.randomUUID()
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every { planRepository.findById(unknownPlanId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.changePlan(unknownPlanId)
        }
    }

    // ── addMarketplace ──────────────────────────────────────────────────

    @Test
    fun `should add marketplace when under limit`() {
        every { organizationRepository.findById(orgId) } returns Optional.of(organization)
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every { marketplaceConfigRepository.countByOrganizationId(orgId) } returns 1
        every {
            marketplaceConfigRepository.findByOrganizationIdAndMarketplaceName(orgId, "WEBMOTORS")
        } returns Optional.empty()
        every { marketplaceConfigRepository.save(any()) } answers { firstArg() }

        val response = service.addMarketplace("WEBMOTORS", 200)

        assertEquals("WEBMOTORS", response.marketplaceName)
        verify { marketplaceConfigRepository.save(any()) }
    }

    @Test
    fun `should throw BusinessException when marketplace limit reached`() {
        every { organizationRepository.findById(orgId) } returns Optional.of(organization)
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every { marketplaceConfigRepository.countByOrganizationId(orgId) } returns 2 // limit is 2

        assertThrows<BusinessException> {
            service.addMarketplace("OLX", 200)
        }
    }

    @Test
    fun `should throw BusinessException when marketplace already exists`() {
        val existingConfig = MarketplaceConfig(
            organization = organization,
            marketplaceName = "WEBMOTORS",
            monthlyLimit = 200
        )
        every { organizationRepository.findById(orgId) } returns Optional.of(organization)
        every { subscriptionRepository.findByOrganizationId(orgId) } returns Optional.of(subscription)
        every { marketplaceConfigRepository.countByOrganizationId(orgId) } returns 1
        every {
            marketplaceConfigRepository.findByOrganizationIdAndMarketplaceName(orgId, "WEBMOTORS")
        } returns Optional.of(existingConfig)

        assertThrows<BusinessException> {
            service.addMarketplace("WEBMOTORS", 200)
        }
    }
}
