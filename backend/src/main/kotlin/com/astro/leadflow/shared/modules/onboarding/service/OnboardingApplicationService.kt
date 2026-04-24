package com.astro.leadflow.shared.modules.onboarding.service

import com.astro.leadflow.shared.exception.DuplicateResourceException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.onboarding.api.requests.SubmitOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationDetailResponse
import com.astro.leadflow.shared.modules.onboarding.api.responses.OnboardingApplicationSummaryResponse
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplication
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplicationRepository
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OnboardingApplicationService(
    private val onboardingRepository: OnboardingApplicationRepository
) {

    @Transactional
    fun submit(request: SubmitOnboardingRequest): UUID {
        if (onboardingRepository.existsByOwnerEmail(request.ownerEmail)) {
            throw DuplicateResourceException("OnboardingApplication", "ownerEmail", request.ownerEmail)
        }
        if (onboardingRepository.existsByCnpj(request.cnpj)) {
            throw DuplicateResourceException("OnboardingApplication", "cnpj", request.cnpj)
        }

        val application = onboardingRepository.save(
            OnboardingApplication(
                companyName = request.companyName,
                tradeName = request.tradeName,
                cnpj = request.cnpj,
                planCode = request.planCode,
                ownerName = request.ownerName,
                ownerCpf = request.ownerCpf,
                ownerEmail = request.ownerEmail,
                ownerPhone = request.ownerPhone,
                cep = request.cep,
                street = request.street,
                number = request.number,
                complement = request.complement,
                neighborhood = request.neighborhood,
                city = request.city,
                state = request.state,
                referralCode = request.referralCode
            )
        )
        return application.id
    }

    @Transactional(readOnly = true)
    fun list(status: OnboardingStatus?, query: String?, pageable: Pageable): Page<OnboardingApplicationSummaryResponse> {
        return onboardingRepository.search(status, query, pageable).map { it.toSummary() }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): OnboardingApplicationDetailResponse {
        val application = onboardingRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("OnboardingApplication", id) }
        return application.toDetail()
    }

    // ── Mapping helpers ──────────────────────────────────────────────────

    private fun OnboardingApplication.toSummary() = OnboardingApplicationSummaryResponse(
        id = id,
        companyName = companyName,
        tradeName = tradeName,
        cnpj = cnpj,
        ownerName = ownerName,
        ownerEmail = ownerEmail,
        planCode = planCode,
        status = status,
        referralCode = referralCode,
        createdAt = createdAt
    )

    private fun OnboardingApplication.toDetail() = OnboardingApplicationDetailResponse(
        id = id,
        companyName = companyName,
        tradeName = tradeName,
        cnpj = cnpj,
        planCode = planCode,
        ownerName = ownerName,
        ownerCpf = ownerCpf,
        ownerEmail = ownerEmail,
        ownerPhone = ownerPhone,
        cep = cep,
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        city = city,
        state = state,
        status = status,
        referralCode = referralCode,
        rejectionReason = rejectionReason,
        approvedOrganizationId = approvedOrganization?.id,
        approvedAt = approvedAt,
        rejectedAt = rejectedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
