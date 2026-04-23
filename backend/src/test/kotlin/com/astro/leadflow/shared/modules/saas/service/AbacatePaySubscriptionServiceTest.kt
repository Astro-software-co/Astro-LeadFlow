package com.astro.leadflow.shared.modules.saas.service

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookCheckout
import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookData
import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookEvent
import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookPayment
import com.astro.leadflow.shared.modules.saas.api.requests.AbacatePayWebhookSubscription
import com.astro.leadflow.shared.modules.saas.api.requests.CreateSubscriptionCheckoutRequest
import com.astro.leadflow.shared.modules.saas.domain.BillingCycle
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntryRepository
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import com.astro.leadflow.shared.modules.saas.infra.AbacateCheckoutResponse
import com.astro.leadflow.shared.modules.saas.infra.AbacateCustomerResponse
import com.astro.leadflow.shared.modules.saas.infra.AbacatePayGateway
import com.astro.leadflow.shared.modules.saas.infra.AbacateProductResponse
import com.astro.leadflow.shared.modules.saas.mapper.SaasMapper
import com.astro.leadflow.shared.util.SecurityUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class AbacatePaySubscriptionServiceTest {

    @MockK private lateinit var subscriptionRepository: SubscriptionRepository
    @MockK private lateinit var planRepository: PlanRepository
    @MockK private lateinit var userRepository: UserRepository
    @MockK private lateinit var billingHistoryEntryRepository: BillingHistoryEntryRepository
    @MockK private lateinit var abacatePayGateway: AbacatePayGateway

    private lateinit var service: AbacatePaySubscriptionService

    private val organization = Organization(name = "Astro Motors", slug = "astro-motors", cnpj = "12.345.678/0001-90")
    private val currentPlan = Plan(
        name = "Básico",
        priceBrl = BigDecimal("149.00"),
        messageLimit = 1000,
        marketplaceLimit = 1,
    )
    private val targetPlan = Plan(
        name = "Profissional",
        priceBrl = BigDecimal("299.00"),
        messageLimit = 3000,
        marketplaceLimit = 3,
        billingCycle = BillingCycle.MONTHLY,
    )
    private val user = User(
        organization = organization,
        name = "Admin Astro",
        email = "admin@astro.test",
        passwordHash = "hash",
        phoneNumber = "(11) 99999-0000",
        role = Role.ADMIN,
    )
    private val subscription = Subscription(
        organization = organization,
        plan = currentPlan,
        status = SubscriptionStatus.ACTIVE,
    )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        service = AbacatePaySubscriptionService(
            subscriptionRepository,
            planRepository,
            userRepository,
            billingHistoryEntryRepository,
            abacatePayGateway,
            SaasMapper(),
            "http://localhost:5173",
        )

        mockkObject(SecurityUtils)
        every { SecurityUtils.getCurrentUser() } returns user
        every { SecurityUtils.getCurrentOrganizationId() } returns organization.id
        every { subscriptionRepository.findByOrganizationId(organization.id) } returns Optional.of(subscription)
        every { userRepository.findByIdWithOrganization(user.id) } returns Optional.of(user)
        every { subscriptionRepository.save(any()) } answers { firstArg() }
        every { planRepository.save(any()) } answers { firstArg() }
        every { billingHistoryEntryRepository.save(any()) } answers { firstArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SecurityUtils)
    }

    @Test
    fun `createCheckout should provision remote resources and return checkout url`() {
        every { planRepository.findById(targetPlan.id) } returns Optional.of(targetPlan)
        every {
            abacatePayGateway.createProduct(any())
        } returns AbacateProductResponse(
            id = "prod_123",
            externalId = "leadflow-plan:${targetPlan.id}",
            name = targetPlan.displayName(),
            price = 29900,
            cycle = "MONTHLY",
        )
        every {
            abacatePayGateway.createCustomer(any())
        } returns AbacateCustomerResponse(
            id = "cust_123",
            email = user.email,
            name = user.name,
        )
        every {
            abacatePayGateway.createSubscriptionCheckout(any())
        } returns AbacateCheckoutResponse(
            id = "bill_123",
            url = "https://app.abacatepay.com/pay/bill_123",
            status = "PENDING",
        )

        val result = service.createCheckout(
            targetPlan.id,
            CreateSubscriptionCheckoutRequest(
                returnUrl = "http://localhost:5173/app/configuracoes",
                completionUrl = "http://localhost:5173/app/configuracoes",
            ),
        )

        assertEquals(targetPlan.id, result.planId)
        assertEquals("https://app.abacatepay.com/pay/bill_123", result.checkoutUrl)
        assertEquals("cust_123", subscription.abacateCustomerId)
        assertEquals("bill_123", subscription.abacateCheckoutId)
        assertEquals("PENDING", subscription.abacateCheckoutStatus)
        verify(exactly = 1) { abacatePayGateway.createProduct(any()) }
        verify(exactly = 1) { abacatePayGateway.createCustomer(any()) }
        verify(exactly = 1) { abacatePayGateway.createSubscriptionCheckout(any()) }
    }

    @Test
    fun `handleWebhook should activate target plan and store billing entry`() {
        every { billingHistoryEntryRepository.findByProviderEventId("evt_1") } returns Optional.empty()
        every { subscriptionRepository.findByAbacateSubscriptionId("subs_123") } returns Optional.empty()
        every { subscriptionRepository.findById(subscription.id) } returns Optional.of(subscription)
        every { planRepository.findById(targetPlan.id) } returns Optional.of(targetPlan)

        val event = AbacatePayWebhookEvent(
            id = "evt_1",
            event = "subscription.completed",
            data = AbacatePayWebhookData(
                subscription = AbacatePayWebhookSubscription(
                    id = "subs_123",
                    amount = 29900,
                    status = "ACTIVE",
                    frequency = "MONTHLY",
                    createdAt = "2026-04-23T12:00:00Z",
                    updatedAt = "2026-04-23T12:00:05Z",
                ),
                payment = AbacatePayWebhookPayment(
                    id = "pay_123",
                    externalId = "leadflow-subscription:${subscription.id}:plan:${targetPlan.id}",
                    amount = 29900,
                    paidAmount = 29900,
                    status = "PAID",
                    receiptUrl = "https://app.abacatepay.com/receipt/pay_123",
                    createdAt = "2026-04-23T12:00:00Z",
                    updatedAt = "2026-04-23T12:00:05Z",
                ),
                checkout = AbacatePayWebhookCheckout(
                    id = "bill_123",
                    externalId = "leadflow-subscription:${subscription.id}:plan:${targetPlan.id}",
                    status = "PAID",
                ),
            ),
        )

        service.handleWebhook(event)

        assertEquals(targetPlan.id, subscription.plan.id)
        assertEquals("subs_123", subscription.abacateSubscriptionId)
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
        assertNull(subscription.abacateCheckoutUrl)
        assertEquals(LocalDate.of(2026, 5, 23), subscription.renewsAt)
        verify(exactly = 1) { billingHistoryEntryRepository.save(any()) }
    }
}
