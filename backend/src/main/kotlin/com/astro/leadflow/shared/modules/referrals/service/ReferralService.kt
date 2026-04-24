package com.astro.leadflow.shared.modules.referrals.service

import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationship
import com.astro.leadflow.shared.modules.referrals.domain.ReferralRelationshipRepository
import com.astro.leadflow.shared.modules.referrals.domain.ReferralStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Tier thresholds for referral commissions:
 *  BRONZE:  0  – 9  approved referrals
 *  SILVER:  10 – 19 approved referrals
 *  GOLD:    20 – 34 approved referrals
 *  DIAMOND: 35+     approved referrals
 */
enum class ReferralTier { BRONZE, SILVER, GOLD, DIAMOND }

@Service
class ReferralService(
    private val referralRepository: ReferralRelationshipRepository
) {

    @Transactional(readOnly = true)
    fun getTier(organizationId: UUID): ReferralTier {
        val count = referralRepository.countByReferrerOrganizationIdAndStatus(
            organizationId,
            ReferralStatus.APPROVED
        )
        return when {
            count >= 35 -> ReferralTier.DIAMOND
            count >= 20 -> ReferralTier.GOLD
            count >= 10 -> ReferralTier.SILVER
            else        -> ReferralTier.BRONZE
        }
    }

    @Transactional(readOnly = true)
    fun listReferrals(organizationId: UUID): List<ReferralRelationship> {
        return referralRepository.findAllByReferrerOrganizationId(organizationId)
    }
}
