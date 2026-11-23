package com.astro.leadflow.shared.modules.admin.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplication
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplicationRepository
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import com.astro.leadflow.shared.modules.saas.domain.BillingCycle
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntry
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntryRepository
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AdminDashboardServiceTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var billingHistoryEntryRepository: BillingHistoryEntryRepository
    private lateinit var onboardingApplicationRepository: OnboardingApplicationRepository
    private lateinit var service: AdminDashboardService

    private val plan = Plan(
        name = "Pro",
        priceBrl = BigDecimal("299.00"),
        messageLimit = 3000,
        marketplaceLimit = 3,
        billingCycle = BillingCycle.MONTHLY
    )
    private val organization = Organization(
        name = "Astro Motors",
        slug = "astro-motors",
        referralCode = "ASTRO123"
    )
    private val subscription = Subscription(
        organization = organization,
        plan = plan,
        status = SubscriptionStatus.ACTIVE,
        renewsAt = LocalDate.now().plusMonths(1),
        createdAt = LocalDateTime.now().minusMonths(2)
    )

    @BeforeEach
    fun setup() {
        subscriptionRepository = mockk()
        billingHistoryEntryRepository = mockk()
        onboardingApplicationRepository = mockk()
        service = AdminDashboardService(
            subscriptionRepository,
            billingHistoryEntryRepository,
            onboardingApplicationRepository
        )
    }

    @Test
    fun `getDashboard returns finance metrics and subscription rows`() {
        val now = YearMonth.now()
        val billing = BillingHistoryEntry(
            subscription = subscription,
            description = "Pro mensal",
            amountBrl = BigDecimal("299.00"),
            status = "PAID",
            billedAt = now.atDay(1),
            issuedAt = now.atDay(1)
        )
        val application = OnboardingApplication(
            companyName = "Pending Motors",
            cnpj = "11222333000181",
            planCode = "BASICO_MONTHLY",
            ownerName = "Ana",
            ownerCpf = "12345678909",
            ownerEmail = "ana@example.com",
            ownerPhone = "11999999999",
            status = OnboardingStatus.PENDING_APPROVAL
        )

        every { subscriptionRepository.findAll() } returns listOf(subscription)
        every { billingHistoryEntryRepository.findAll() } returns listOf(billing)
        every { onboardingApplicationRepository.findAll() } returns listOf(application)

        val dashboard = service.getDashboard()

        assertEquals(BigDecimal("299.00"), dashboard.metrics.mrrAtual)
        assertEquals(BigDecimal("299.00"), dashboard.metrics.receitaDoMes)
        assertEquals(1, dashboard.metrics.assinaturasAtivas)
        assertEquals(1, dashboard.metrics.cadastrosPendentesDeAprovacao)
        assertEquals("Astro Motors", dashboard.subscriptions.first().empresa)
        assertEquals("Onboarding", dashboard.subscriptions.first().origem)
        assertTrue(dashboard.revenueLast12Months.size == 12)
        assertEquals(setOf(3, 6, 12), dashboard.projections.map { it.months }.toSet())
    }

    @Test
    fun `controller is protected by ASTRO_ADMIN role`() {
        val controller = com.astro.leadflow.shared.modules.admin.api.AdminDashboardController::class.java
        val mapping = controller.getAnnotation(org.springframework.web.bind.annotation.RequestMapping::class.java)
        val preAuthorize = controller.getAnnotation(org.springframework.security.access.prepost.PreAuthorize::class.java)

        assertEquals(listOf("/api/v1/admin/dashboard"), mapping.value.toList())
        assertEquals("hasRole('ASTRO_ADMIN')", preAuthorize.value)
    }
}
