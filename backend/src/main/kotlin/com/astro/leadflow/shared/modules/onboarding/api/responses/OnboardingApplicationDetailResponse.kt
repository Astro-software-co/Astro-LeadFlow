package com.astro.leadflow.shared.modules.onboarding.api.responses

import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import java.time.LocalDateTime
import java.util.UUID

data class OnboardingApplicationDetailResponse(
    val id: UUID,
    val companyName: String,
    val tradeName: String?,
    val cnpj: String,
    val planCode: String,
    val ownerName: String,
    val ownerCpf: String,
    val ownerEmail: String,
    val ownerPhone: String,
    val cep: String?,
    val street: String?,
    val number: String?,
    val complement: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val status: OnboardingStatus,
    val referralCode: String?,
    val rejectionReason: String?,
    val approvedOrganizationId: UUID?,
    val approvedAt: LocalDateTime?,
    val rejectedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
