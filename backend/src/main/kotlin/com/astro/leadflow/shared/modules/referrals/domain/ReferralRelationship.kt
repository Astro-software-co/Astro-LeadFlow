package com.astro.leadflow.shared.modules.referrals.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.onboarding.domain.OnboardingApplication
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "referral_relationships",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_referral_relationships_referred_org",
            columnNames = ["referred_organization_id"]
        )
    ]
)
class ReferralRelationship(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_organization_id", nullable = false)
    var referrerOrganization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_organization_id", nullable = false)
    var referredOrganization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_application_id")
    var onboardingApplication: OnboardingApplication? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ReferralStatus = ReferralStatus.PENDING,

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    var commissionRate: BigDecimal = BigDecimal("0.0800"),

    @Column(name = "commission_amount_brl", precision = 10, scale = 2)
    var commissionAmountBrl: BigDecimal? = null,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
