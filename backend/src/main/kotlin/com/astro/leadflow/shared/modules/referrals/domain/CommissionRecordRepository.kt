package com.astro.leadflow.shared.modules.referrals.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CommissionRecordRepository : JpaRepository<CommissionRecord, UUID> {
    fun findByReferralRelationshipId(referralRelationshipId: UUID): Optional<CommissionRecord>
    fun existsByReferralRelationshipId(referralRelationshipId: UUID): Boolean
}
