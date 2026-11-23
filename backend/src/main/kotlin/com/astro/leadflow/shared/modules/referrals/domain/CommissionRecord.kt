package com.astro.leadflow.shared.modules.referrals.domain

import com.astro.leadflow.shared.modules.saas.domain.BillingHistoryEntry
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "commission_records",
    uniqueConstraints = [UniqueConstraint(name = "uq_commission_records_relationship", columnNames = ["referral_relationship_id"])]
)
class CommissionRecord(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_relationship_id", nullable = false)
    val referralRelationship: ReferralRelationship,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_history_entry_id")
    val billingHistoryEntry: BillingHistoryEntry? = null,

    @Column(name = "amount_brl", nullable = false, precision = 10, scale = 2)
    val amountBrl: BigDecimal,

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    val commissionRate: BigDecimal,

    @Column(name = "tier", nullable = false, length = 20)
    val tier: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
