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
import com.astro.leadflow.shared.modules.onboarding.api.responses.ApproveOnboardingResponse
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplicationRepository
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingStatus
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationship
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationshipRepository
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class OnboardingApprovalService(
    private val onboardingRepository: OnboardingApplicationRepository,
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val referralRepository: ReferralRelationshipRepository,
    private val passwordEncoder: PasswordEncoder,
    private val planRepository: PlanRepository,
    private val subscriptionRepository: SubscriptionRepository
) {

    @Transactional
    fun approve(applicationId: UUID): ApproveOnboardingResponse {
        val application = onboardingRepository.findById(applicationId)
            .orElseThrow { ResourceNotFoundException("OnboardingApplication", applicationId) }

        if (!application.status.isPendingApproval()) {
            throw BusinessException(
                "Solicitação de onboarding não está pendente. Status atual: ${application.status}"
            )
        }

        if (userRepository.existsByEmail(application.ownerEmail)) {
            throw DuplicateResourceException("User", "email", application.ownerEmail)
        }

        // 1. Create organization with unique referral code
        val referralCode = generateUniqueReferralCode()
        val slug = generateSlug(application.companyName)

        val organization = organizationRepository.save(
            Organization(
                name = application.companyName,
                slug = slug,
                cnpj = application.cnpj,
                referralCode = referralCode
            )
        )

        // 2. Create owner user with ADMIN role and PENDING_VERIFICATION status
        val tempPassword = UUID.randomUUID().toString().replace("-", "").take(16)
        userRepository.save(
            User(
                organization = organization,
                name = application.ownerName,
                email = application.ownerEmail,
                passwordHash = passwordEncoder.encode(tempPassword),
                role = Role.ADMIN,
                status = UserStatus.PENDING_VERIFICATION
            )
        )

        // 3. Resolve plan and create SaaS subscription
        val plan = planRepository.findByCode(application.planCode)
            .orElseThrow { BusinessException("Código de plano inválido: ${application.planCode}") }

        subscriptionRepository.save(Subscription(organization = organization, plan = plan))

        // 4. Link referral relationship if submitted referral code resolves to an existing org
        val submittedReferralCode = application.referralCode
        if (!submittedReferralCode.isNullOrBlank()) {
            organizationRepository.findByReferralCode(submittedReferralCode).ifPresent { referrerOrg ->
                referralRepository.save(
                    ReferralRelationship(
                        referrerOrganization = referrerOrg,
                        referredOrganization = organization,
                        onboardingApplication = application
                    )
                )
            }
        }

        // 5. Mark application as approved
        application.status = OnboardingStatus.APPROVED
        application.approvedOrganization = organization
        application.approvedAt = LocalDateTime.now()
        onboardingRepository.save(application)

        return ApproveOnboardingResponse(
            organizationId = organization.id,
            organizationName = organization.name,
            ownerEmail = application.ownerEmail,
            referralCode = referralCode
        )
    }

    @Transactional
    fun reject(applicationId: UUID, rejectionReason: String) {
        if (rejectionReason.length < 10) {
            throw BusinessException("Motivo da rejeição deve ter pelo menos 10 caracteres.")
        }

        val application = onboardingRepository.findById(applicationId)
            .orElseThrow { ResourceNotFoundException("OnboardingApplication", applicationId) }

        if (!application.status.isPendingApproval()) {
            throw BusinessException(
                "Solicitação de onboarding não está pendente. Status atual: ${application.status}"
            )
        }

        application.status = OnboardingStatus.REJECTED
        application.rejectionReason = rejectionReason
        application.rejectedAt = LocalDateTime.now()
        onboardingRepository.save(application)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun generateUniqueReferralCode(): String {
        var code: String
        do {
            code = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        } while (organizationRepository.findByReferralCode(code).isPresent)
        return code
    }

    private fun generateSlug(companyName: String): String {
        val base = companyName
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
            .take(50)
        val suffix = UUID.randomUUID().toString().take(8)
        return "$base-$suffix"
    }
}
