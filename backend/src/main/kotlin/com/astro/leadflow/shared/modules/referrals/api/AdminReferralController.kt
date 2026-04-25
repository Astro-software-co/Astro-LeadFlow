package com.astro.leadflow.shared.modules.referrals.api

import com.astro.leadflow.shared.modules.referrals.api.responses.AdminReferralSummaryResponse
import com.astro.leadflow.shared.modules.referrals.service.ReferralService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/referral")
@PreAuthorize("hasRole('ASTRO_ADMIN')")
@Tag(name = "Referral — Admin", description = "Visão administrativa do programa de indicação")
class AdminReferralController(private val referralService: ReferralService) {

    @GetMapping("/summary")
    fun getSummary(): AdminReferralSummaryResponse = referralService.getAdminSummary()
}
