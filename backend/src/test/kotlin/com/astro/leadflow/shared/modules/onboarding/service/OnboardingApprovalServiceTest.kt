package com.astro.leadflow.shared.modules.onboarding.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.DuplicateResourceException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplication
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplicationRepository
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationship
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationshipRepository
import com.astro.leadflow.shared.modules.saas.domain.BillingCycle
import com.astro.leadflow.shared.modules.saas.domain.Plan
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class OnboardingApprovalServiceTest {

    private lateinit var onboardingRepository: OnboardingApplicationRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var referralRepository: ReferralRelationshipRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var planRepository: PlanRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var service: OnboardingApprovalService

    private val applicationId = UUID.randomUUID()
    private val referrerOrgId = UUID.randomUUID()
    private val referrerCode = "REFCODE12345678"

    private val referrerOrg = Organization(
        id = referrerOrgId,
        name = "Referrer Org",
        slug = "referrer-org",
        referralCode = referrerCode
    )

    private val testPlan = Plan(
        name = "Básico",
        priceBrl = BigDecimal("99.90"),
        messageLimit = 500,
        marketplaceLimit = 2,
        code = "BASICO_MONTHLY",
        billingCycle = BillingCycle.MONTHLY
    )

    private fun pendingApplication(referralCode: String? = null) = OnboardingApplication(
        id = applicationId,
        companyName = "Nova Concessionária Ltda",
        cnpj = "12345678000190",
        planCode = "BASICO_MONTHLY",
        ownerName = "João Silva",
        ownerCpf = "12345678900",
        ownerEmail = "joao@nova.com",
        ownerPhone = "11999999999",
        status = OnboardingStatus.PENDING_APPROVAL,
        referralCode = referralCode
    )

    @BeforeEach
    fun setup() {
        onboardingRepository = mockk()
        organizationRepository = mockk()
        userRepository = mockk()
        referralRepository = mockk()
        passwordEncoder = mockk()
        planRepository = mockk()
        subscriptionRepository = mockk()

        service = OnboardingApprovalService(
            onboardingRepository,
            organizationRepository,
            userRepository,
            referralRepository,
            passwordEncoder,
            planRepository,
            subscriptionRepository
        )
    }

    private fun stubHappyPath(application: OnboardingApplication, hasReferrer: Boolean = false) {
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)
        every { userRepository.existsByEmail(application.ownerEmail) } returns false
        every { organizationRepository.findByReferralCode(any()) } returns Optional.empty()
        if (hasReferrer) {
            every { organizationRepository.findByReferralCode(referrerCode) } returns Optional.of(referrerOrg)
        }
        every { organizationRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }
        every { planRepository.findByCode("BASICO_MONTHLY") } returns Optional.of(testPlan)
        every { subscriptionRepository.save(any()) } answers { firstArg() }
        every { referralRepository.save(any()) } answers { firstArg() }
        every { onboardingRepository.save(any()) } answers { firstArg() }
        every { passwordEncoder.encode(any()) } returns "hashed-password"
    }

    // ── approve ──────────────────────────────────────────────────────────

    @Test
    fun `approve creates organization and owner user with PENDING_VERIFICATION`() {
        val application = pendingApplication()
        stubHappyPath(application)

        val response = service.approve(applicationId)

        assertNotNull(response.organizationId)
        assertEquals(application.companyName, response.organizationName)
        assertEquals(application.ownerEmail, response.ownerEmail)
        assertNotNull(response.referralCode)

        val userSlot = slot<User>()
        verify { userRepository.save(capture(userSlot)) }
        assertEquals(Role.ADMIN, userSlot.captured.role)
        assertEquals(UserStatus.PENDING_VERIFICATION, userSlot.captured.status)
    }

    @Test
    fun `approve generates unique referral code on the organization`() {
        val application = pendingApplication()
        stubHappyPath(application)

        val response = service.approve(applicationId)

        assertNotNull(response.referralCode, "referralCode must not be null on approve response")
        assert(response.referralCode.isNotBlank()) { "referralCode must not be blank" }
    }

    @Test
    fun `approve creates Subscription linked to organization and plan`() {
        val application = pendingApplication()
        stubHappyPath(application)

        service.approve(applicationId)

        val subSlot = slot<Subscription>()
        verify { subscriptionRepository.save(capture(subSlot)) }
        assertNotNull(subSlot.captured.organization)
        assertEquals(testPlan, subSlot.captured.plan)
    }

    @Test
    fun `approve throws BusinessException when plan code is unknown`() {
        val application = pendingApplication()
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)
        every { userRepository.existsByEmail(application.ownerEmail) } returns false
        every { organizationRepository.findByReferralCode(any()) } returns Optional.empty()
        every { organizationRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }
        every { planRepository.findByCode("BASICO_MONTHLY") } returns Optional.empty()
        every { passwordEncoder.encode(any()) } returns "hashed-password"

        assertThrows<BusinessException> {
            service.approve(applicationId)
        }
    }

    @Test
    fun `approve creates ReferralRelationship when submitted referralCode matches existing org`() {
        val application = pendingApplication(referralCode = referrerCode)
        stubHappyPath(application, hasReferrer = true)

        service.approve(applicationId)

        val relSlot = slot<ReferralRelationship>()
        verify { referralRepository.save(capture(relSlot)) }
        assertEquals(referrerOrg, relSlot.captured.referrerOrganization)
        assertEquals(application, relSlot.captured.onboardingApplication)
    }

    @Test
    fun `approve does not create ReferralRelationship when no referralCode submitted`() {
        val application = pendingApplication(referralCode = null)
        stubHappyPath(application)

        service.approve(applicationId)

        verify(exactly = 0) { referralRepository.save(any()) }
    }

    @Test
    fun `approve does not create ReferralRelationship when submitted referralCode is unknown`() {
        val application = pendingApplication(referralCode = "UNKNOWN_CODE_XYZ")
        stubHappyPath(application)

        service.approve(applicationId)

        verify(exactly = 0) { referralRepository.save(any()) }
    }

    @Test
    fun `approve throws ResourceNotFoundException when application not found`() {
        every { onboardingRepository.findById(applicationId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.approve(applicationId)
        }
    }

    @Test
    fun `approve throws BusinessException when application is not PENDING`() {
        val application = pendingApplication().apply { status = OnboardingStatus.APPROVED }
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)

        assertThrows<BusinessException> {
            service.approve(applicationId)
        }
    }

    @Test
    fun `approve accepts legacy PENDING status for compatibility`() {
        val application = pendingApplication().apply { status = OnboardingStatus.PENDING }
        stubHappyPath(application)

        service.approve(applicationId)

        assertEquals(OnboardingStatus.APPROVED, application.status)
        verify { subscriptionRepository.save(any()) }
    }

    @Test
    fun `approve throws DuplicateResourceException when owner email already registered`() {
        val application = pendingApplication()
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)
        every { userRepository.existsByEmail(application.ownerEmail) } returns true

        assertThrows<DuplicateResourceException> {
            service.approve(applicationId)
        }
    }

    // ── reject ────────────────────────────────────────────────────────────

    @Test
    fun `reject marks application as REJECTED and sets rejectionReason`() {
        val application = pendingApplication()
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)
        every { onboardingRepository.save(any()) } answers { firstArg() }

        service.reject(applicationId, "Documentação insuficiente para análise.")

        assertEquals(OnboardingStatus.REJECTED, application.status)
        assertEquals("Documentação insuficiente para análise.", application.rejectionReason)
        verify { onboardingRepository.save(application) }
    }

    @Test
    fun `reject throws BusinessException when rejectionReason is too short`() {
        val application = pendingApplication()
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)

        assertThrows<BusinessException> {
            service.reject(applicationId, "Curto")
        }
    }

    @Test
    fun `reject throws BusinessException when application is not PENDING`() {
        val application = pendingApplication().apply { status = OnboardingStatus.APPROVED }
        every { onboardingRepository.findById(applicationId) } returns Optional.of(application)

        assertThrows<BusinessException> {
            service.reject(applicationId, "Documentação insuficiente para análise.")
        }
    }

    @Test
    fun `reject throws ResourceNotFoundException when application not found`() {
        every { onboardingRepository.findById(applicationId) } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            service.reject(applicationId, "Qualquer motivo valido aqui.")
        }
    }
}
