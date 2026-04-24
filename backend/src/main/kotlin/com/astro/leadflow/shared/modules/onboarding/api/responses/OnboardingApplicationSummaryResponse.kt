package com.astro.leadflow.shared.modules.onboarding.api.responses

import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import java.time.LocalDateTime
import java.util.UUID

data class OnboardingApplicationSummaryResponse(
    val id: UUID,
    val companyName: String,
    val tradeName: String?,
    val cnpj: String,
    val ownerName: String,
    val ownerEmail: String,
    val planCode: String,
    val status: OnboardingStatus,
    val referralCode: String?,
    val createdAt: LocalDateTime
)
