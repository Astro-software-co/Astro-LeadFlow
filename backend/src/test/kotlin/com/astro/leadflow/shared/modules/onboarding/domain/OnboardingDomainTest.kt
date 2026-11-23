package com.astro.leadflow.shared.modules.onboarding.domain

import com.astro.leadflow.shared.modules.onboarding.api.requests.SubmitOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.api.requests.RejectOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationSummaryResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationDetailResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.ApproveOnboardingResponse
import com.astro.leadflow.shared.modules.referrals.domain.ReferralStatus
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OnboardingDomainTest {

    @Test
    fun `OnboardingStatus enum contains prompt statuses plus legacy PENDING alias`() {
        val names = OnboardingStatus.entries.map { it.name }
        assert(names.contains("PENDING_APPROVAL")) { "Missing PENDING_APPROVAL" }
        assert(names.contains("PENDING")) { "Missing PENDING" }
        assert(names.contains("APPROVED")) { "Missing APPROVED" }
        assert(names.contains("REJECTED")) { "Missing REJECTED" }
        assertEquals(OnboardingStatus.PENDING_APPROVAL, OnboardingStatus.PENDING.canonical())
    }

    @Test
    fun `SubmitOnboardingRequest has optional referralCode`() {
        val req = SubmitOnboardingRequest(
            companyName = "Test Ltda",
            tradeName = null,
            cnpj = "12345678000190",
            planCode = "BASICO_MONTHLY",
            ownerName = "Test Owner",
            ownerCpf = "12345678900",
            ownerEmail = "test@test.com",
            ownerPhone = "11999999999",
            referralCode = null
        )
        assertNull(req.referralCode, "referralCode should be nullable")
    }

    @Test
    fun `SubmitOnboardingRequest accepts referralCode when provided`() {
        val req = SubmitOnboardingRequest(
            companyName = "Test Ltda",
            tradeName = null,
            cnpj = "12345678000190",
            planCode = "BASICO_MONTHLY",
            ownerName = "Test Owner",
            ownerCpf = "12345678900",
            ownerEmail = "test@test.com",
            ownerPhone = "11999999999",
            referralCode = "ABC123"
        )
        assertEquals("ABC123", req.referralCode)
    }

    @Test
    fun `RejectOnboardingRequest has rejectionReason field`() {
        val req = RejectOnboardingRequest(rejectionReason = "Documentação incompleta.")
        assertNotNull(req.rejectionReason)
    }

    @Test
    fun `ApproveOnboardingResponse contains referralCode`() {
        val response = ApproveOnboardingResponse(
            organizationId = UUID.randomUUID(),
            organizationName = "Test Org",
            ownerEmail = "owner@test.com",
            referralCode = "XYZ789"
        )
        assertEquals("XYZ789", response.referralCode)
    }

    @Test
    fun `ReferralStatus enum contains PENDING APPROVED REJECTED`() {
        val names = ReferralStatus.entries.map { it.name }
        assert(names.contains("PENDING")) { "Missing PENDING" }
        assert(names.contains("APPROVED")) { "Missing APPROVED" }
        assert(names.contains("REJECTED")) { "Missing REJECTED" }
    }
}
