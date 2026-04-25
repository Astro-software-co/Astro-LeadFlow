package com.astro.leadflow.shared.modules.referrals.api

import com.astro.leadflow.shared.modules.referrals.api.responses.ReferralBalanceResponse
import com.astro.leadflow.shared.modules.referrals.api.responses.ReferralSummaryResponse
import com.astro.leadflow.shared.modules.referrals.api.responses.ReferredCompanyResponse
import com.astro.leadflow.shared.modules.referrals.service.ReferralService
import com.astro.leadflow.shared.util.SecurityUtils
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/referral", "/api/v1/referral"])
@PreAuthorize("hasRole('DEALERSHIP')")
@Tag(name = "Referral — Dealership", description = "Programa de indicação para concessionárias")
class ReferralController(private val referralService: ReferralService) {

    @GetMapping("/my-code")
    fun getMyCode(): ReferralSummaryResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return referralService.getSummary(orgId)
    }

    @GetMapping("/my-referrals")
    fun getMyReferrals(): List<ReferredCompanyResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return referralService.getReferredCompanies(orgId)
    }

    @GetMapping("/my-balance")
    fun getMyBalance(): ReferralBalanceResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return referralService.getBalance(orgId)
    }
}
