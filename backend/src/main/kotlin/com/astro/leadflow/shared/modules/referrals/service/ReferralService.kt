package com.astro.leadflow.shared.modules.referrals.service

import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.referrals.api.responses.AdminReferralSummaryResponse
import com.astro.leadflow.shared.modules.referrals.api.responses.ReferralBalanceResponse
import com.astro.leadflow.shared.modules.referrals.api.responses.ReferralSummaryResponse
import com.astro.leadflow.shared.modules.referrals.api.responses.ReferredCompanyResponse
import com.astro.leadflow.shared.modules.referrals.api.responses.TopReferrerResponse
import com.astro.leadflow.shared.modules.referrals.domain.CommissionRecord
import com.astro.leadflow.shared.modules.referrals.domain.CommissionRecordRepository
import com.astro.leadflow.shared.modules.referrals.domain.ReferralLink
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationship
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationshipRepository
import com.astro.leadflow.shared.modules.referrals.domain.ReferralStatus
import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Tier thresholds and commission rates:
 *  BRONZE:  0  – 9  approved referrals → 8%
 *  SILVER:  10 – 19 approved referrals → 12%
 *  GOLD:    20 – 34 approved referrals → 16%
 *  DIAMOND: 35+     approved referrals → 16%
 */
enum class ReferralTier(val rate: BigDecimal, val nextThreshold: Int) {
    BRONZE(BigDecimal("0.0800"), 10),
    SILVER(BigDecimal("0.1200"), 20),
    GOLD(BigDecimal("0.1600"), 35),
    DIAMOND(BigDecimal("0.1600"), Int.MAX_VALUE)
}

@Service
class ReferralService(
    private val referralRepository: ReferralRelationshipRepository,
    private val commissionRepository: CommissionRecordRepository,
    private val organizationRepository: OrganizationRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional(readOnly = true)
    fun getTier(organizationId: UUID): ReferralTier {
        val count = referralRepository.countByReferrerOrganizationIdAndStatus(
            organizationId, ReferralStatus.APPROVED
        )
        return tierForCount(count)
    }

    @Transactional(readOnly = true)
    fun listReferrals(organizationId: UUID): List<ReferralRelationship> =
        referralRepository.findAllByReferrerOrganizationId(organizationId)

    @Transactional(readOnly = true)
    fun getSummary(organizationId: UUID): ReferralSummaryResponse {
        val org = organizationRepository.findById(organizationId)
            .orElseThrow { IllegalStateException("Organization not found") }
        val code = org.referralCode ?: ""
        val referrals = referralRepository.findAllByReferrerOrganizationId(organizationId)
        val approvedCount = referrals.count { it.status == ReferralStatus.APPROVED }.toLong()
        val tier = tierForCount(approvedCount)
        val balance = commissionRepository.findAll()
            .filter { it.referralRelationship.referrerOrganization.id == organizationId }
            .sumOf { it.amountBrl }

        return ReferralSummaryResponse(
            code = code,
            referralLink = ReferralLink.fromCode(code).url,
            approvedCount = approvedCount,
            pendingCount = referrals.count { it.status == ReferralStatus.PENDING }.toLong(),
            currentTier = tier.name,
            commissionRate = tier.rate,
            nextTierThreshold = tier.nextThreshold,
            balanceBrl = balance,
            referrals = referrals.map { it.toResponse() }
        )
    }

    @Transactional(readOnly = true)
    fun getReferredCompanies(organizationId: UUID): List<ReferredCompanyResponse> =
        referralRepository.findAllByReferrerOrganizationId(organizationId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getBalance(organizationId: UUID): ReferralBalanceResponse {
        val balance = commissionRepository.findAll()
            .filter { it.referralRelationship.referrerOrganization.id == organizationId }
            .sumOf { it.amountBrl }
        return ReferralBalanceResponse(
            balanceBrl = balance,
            pendingBrl = BigDecimal.ZERO,
            withdrawalAvailable = false
        )
    }

    @Transactional(readOnly = true)
    fun getAdminSummary(): AdminReferralSummaryResponse {
        val allRelationships = referralRepository.findAll()
        val allCommissions = commissionRepository.findAll()
        val totalPaid = allCommissions.sumOf { it.amountBrl }

        val topReferrers = allRelationships
            .groupBy { it.referrerOrganization }
            .map { (org, rels) ->
                val approved = rels.count { it.status == ReferralStatus.APPROVED }.toLong()
                val tier = tierForCount(approved)
                val commissionTotal = allCommissions
                    .filter { it.referralRelationship.referrerOrganization.id == org.id }
                    .sumOf { it.amountBrl }
                TopReferrerResponse(
                    companyName = org.name,
                    referralCode = org.referralCode ?: "",
                    approvedCount = approved,
                    tier = tier.name,
                    totalCommissionBrl = commissionTotal
                )
            }
            .sortedByDescending { it.approvedCount }
            .take(20)

        return AdminReferralSummaryResponse(
            totalReferrers = allRelationships.map { it.referrerOrganization.id }.distinct().size.toLong(),
            totalReferrals = allRelationships.size.toLong(),
            totalCommissionPaidBrl = totalPaid,
            pendingCommissionBrl = BigDecimal.ZERO,
            topReferrers = topReferrers
        )
    }

    /**
     * Generates a commission record for the referrer when the referred dealership
     * makes its FIRST paid billing event. Idempotent — subsequent payments do nothing.
     */
    @Transactional
    fun generateCommissionIfFirstPayment(
        referredOrganizationId: UUID,
        billingEntry: BillingHistoryEntry
    ) {
        val relationship = referralRepository
            .findByReferredOrganizationId(referredOrganizationId)
            .orElse(null) ?: return

        if (commissionRepository.existsByReferralRelationshipId(relationship.id)) {
            log.debug("Commission already recorded for relationship {}", relationship.id)
            return
        }

        val referrerId = relationship.referrerOrganization.id
        val approvedCount = referralRepository.countByReferrerOrganizationIdAndStatus(
            referrerId, ReferralStatus.APPROVED
        )
        val tier = tierForCount(approvedCount)
        val commissionAmount = billingEntry.amountBrl
            .multiply(tier.rate)
            .setScale(2, RoundingMode.HALF_UP)

        commissionRepository.save(
            CommissionRecord(
                referralRelationship = relationship,
                billingHistoryEntry = billingEntry,
                amountBrl = commissionAmount,
                commissionRate = tier.rate,
                tier = tier.name
            )
        )
        log.info("Commission recorded: relationship={} amount={} tier={}", relationship.id, commissionAmount, tier.name)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun tierForCount(count: Long) = when {
        count >= 35 -> ReferralTier.DIAMOND
        count >= 20 -> ReferralTier.GOLD
        count >= 10 -> ReferralTier.SILVER
        else        -> ReferralTier.BRONZE
    }

    private fun ReferralRelationship.toResponse() = ReferredCompanyResponse(
        organizationId = referredOrganization.id,
        companyName = referredOrganization.name,
        status = status.name,
        planCode = null,
        commissionBrl = commissionAmountBrl,
        approvedAt = approvedAt
    )
}
