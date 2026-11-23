package com.astro.leadflow.shared.modules.referrals.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface ReferralRelationshipRepository : JpaRepository<ReferralRelationship, UUID> {

    fun findAllByReferrerOrganizationId(referrerOrganizationId: UUID): List<ReferralRelationship>

    fun findAllByReferrerOrganizationIdAndStatus(
        referrerOrganizationId: UUID,
        status: ReferralStatus
    ): List<ReferralRelationship>

    fun findByReferredOrganizationId(referredOrganizationId: UUID): Optional<ReferralRelationship>

    fun existsByReferredOrganizationId(referredOrganizationId: UUID): Boolean

    fun countByReferrerOrganizationIdAndStatus(
        referrerOrganizationId: UUID,
        status: ReferralStatus
    ): Long
}
