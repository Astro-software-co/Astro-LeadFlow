package com.astro.leadflow.shared.modules.onboarding.api

import com.astro.leadflow.shared.modules.onboarding.api.requests.RejectOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.api.responses.ApproveOnboardingResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationDetailResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationSummaryResponse
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import com.astro.leadflow.shared.modules.onboarding.service.OnboardingApplicationService
import com.astro.leadflow.shared.modules.onboarding.service.OnboardingApprovalService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.UUID

class AdminOnboardingControllerTest {

    @MockK
    lateinit var onboardingService: OnboardingApplicationService

    @MockK
    lateinit var approvalService: OnboardingApprovalService

    private lateinit var controller: AdminOnboardingController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        controller = AdminOnboardingController(onboardingService, approvalService)
    }

    private fun summaryResponse(id: UUID = UUID.randomUUID()) = OnboardingApplicationSummaryResponse(
        id = id,
        companyName = "Test Org",
        tradeName = null,
        cnpj = "12345678000190",
        ownerName = "Test Owner",
        ownerEmail = "owner@test.com",
        planCode = "BASICO_MONTHLY",
        ownerPhone = "11999999999",
        status = OnboardingStatus.PENDING_APPROVAL,
        referralCode = null,
        createdAt = LocalDateTime.now()
    )

    private fun detailResponse(id: UUID = UUID.randomUUID()) = OnboardingApplicationDetailResponse(
        id = id,
        companyName = "Test Org",
        tradeName = null,
        cnpj = "12345678000190",
        planCode = "BASICO_MONTHLY",
        ownerName = "Test Owner",
        ownerCpf = "12345678900",
        ownerEmail = "owner@test.com",
        ownerPhone = "11999999999",
        ownerRole = "Socio administrador",
        dealershipSegment = "Seminovos",
        branchCount = 1,
        employeeCount = 12,
        stockCount = 45,
        monthlyLeadsAvg = 220,
        marketYears = 8,
        website = "https://test.example",
        intendedMarketplaces = "WEBMOTORS,OLX",
        otherMarketplaces = "Instagram",
        cep = null,
        street = null,
        number = null,
        complement = null,
        neighborhood = null,
        city = null,
        state = null,
        status = OnboardingStatus.PENDING_APPROVAL,
        referralCode = null,
        rejectionReason = null,
        approvedOrganizationId = null,
        approvedAt = null,
        rejectedAt = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Test
    fun `list should return paginated applications`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(summaryResponse(), summaryResponse()), pageable, 2)
        every { onboardingService.list(null, null, pageable) } returns page

        val result = controller.list(null, null, pageable)

        assertEquals(2, result.totalElements)
    }

    @Test
    fun `list with status filter delegates status to service`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(summaryResponse()), pageable, 1)
        every { onboardingService.list(OnboardingStatus.PENDING_APPROVAL, null, pageable) } returns page

        val result = controller.list(OnboardingStatus.PENDING_APPROVAL, null, pageable)

        assertEquals(1, result.totalElements)
        verify { onboardingService.list(OnboardingStatus.PENDING_APPROVAL, null, pageable) }
    }

    @Test
    fun `controller exposes both admin onboarding route aliases`() {
        val mapping = AdminOnboardingController::class.java.getAnnotation(
            org.springframework.web.bind.annotation.RequestMapping::class.java
        )

        assertNotNull(mapping)
        assertEquals(
            setOf("/api/v1/admin/onboarding", "/api/admin/onboarding"),
            mapping.value.toSet()
        )
    }

    @Test
    fun `getById should return application detail`() {
        val id = UUID.randomUUID()
        val detail = detailResponse(id)
        every { onboardingService.getById(id) } returns detail

        val result = controller.getById(id)

        assertEquals(id, result.id)
    }

    @Test
    fun `approve should delegate to approvalService and return referralCode`() {
        val id = UUID.randomUUID()
        val approveResponse = ApproveOnboardingResponse(
            organizationId = UUID.randomUUID(),
            organizationName = "Test Org",
            ownerEmail = "owner@test.com",
            referralCode = "GENERATED123456"
        )
        every { approvalService.approve(id) } returns approveResponse

        val result = controller.approve(id)

        assertEquals("GENERATED123456", result.referralCode)
        assertNotNull(result.organizationId)
        verify(exactly = 1) { approvalService.approve(id) }
    }

    @Test
    fun `reject should delegate to approvalService and return 204`() {
        val id = UUID.randomUUID()
        val request = RejectOnboardingRequest(rejectionReason = "Documentação insuficiente para análise.")
        every { approvalService.reject(id, request.rejectionReason) } just runs

        controller.reject(id, request)

        verify(exactly = 1) { approvalService.reject(id, "Documentação insuficiente para análise.") }
    }

    @Test
    fun `admin list endpoint returns non-admin access info for RBAC verification`() {
        // RBAC is enforced at SecurityConfig level (hasRole ASTRO_ADMIN)
        // This test verifies the controller uses PreAuthorize annotation pattern
        // by verifying the annotation presence through class declaration
        val annotations = AdminOnboardingController::class.java.getAnnotation(
            org.springframework.security.access.prepost.PreAuthorize::class.java
        )
        assertNotNull(annotations, "AdminOnboardingController must be annotated with @PreAuthorize")
    }
}
