package com.astro.leadflow.shared.modules.onboarding.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "onboarding_applications")
class OnboardingApplication(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    // Company info
    @Column(name = "company_name", nullable = false, length = 255)
    var companyName: String,

    @Column(name = "trade_name", length = 255)
    var tradeName: String? = null,

    @Column(name = "cnpj", nullable = false, length = 20)
    var cnpj: String,

    @Column(name = "plan_code", nullable = false, length = 80)
    var planCode: String,

    // Owner info
    @Column(name = "owner_name", nullable = false, length = 255)
    var ownerName: String,

    @Column(name = "owner_cpf", nullable = false, length = 14)
    var ownerCpf: String,

    @Column(name = "owner_email", nullable = false, length = 255)
    var ownerEmail: String,

    @Column(name = "owner_phone", nullable = false, length = 20)
    var ownerPhone: String,

    // Address
    @Column(name = "cep", length = 10)
    var cep: String? = null,

    @Column(name = "street", length = 255)
    var street: String? = null,

    @Column(name = "number", length = 20)
    var number: String? = null,

    @Column(name = "complement", length = 100)
    var complement: String? = null,

    @Column(name = "neighborhood", length = 100)
    var neighborhood: String? = null,

    @Column(name = "city", length = 100)
    var city: String? = null,

    @Column(name = "state", length = 2)
    var state: String? = null,

    // Extended owner profile
    @Column(name = "owner_role", length = 100)
    var ownerRole: String? = null,

    // Extended dealership profile
    @Column(name = "dealership_segment", length = 100)
    var dealershipSegment: String? = null,

    @Column(name = "branch_count")
    var branchCount: Int? = null,

    @Column(name = "employee_count")
    var employeeCount: Int? = null,

    @Column(name = "stock_count")
    var stockCount: Int? = null,

    @Column(name = "monthly_leads_avg")
    var monthlyLeadsAvg: Int? = null,

    @Column(name = "market_years")
    var marketYears: Int? = null,

    @Column(name = "website", length = 255)
    var website: String? = null,

    // Marketplace intent — stored as comma-separated enum names
    @Column(name = "intended_marketplaces", columnDefinition = "TEXT")
    var intendedMarketplaces: String? = null,

    @Column(name = "other_marketplaces", columnDefinition = "TEXT")
    var otherMarketplaces: String? = null,

    // Workflow
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OnboardingStatus = OnboardingStatus.PENDING,

    @Column(name = "referral_code", length = 32)
    var referralCode: String? = null,

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    var rejectionReason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_organization_id")
    var approvedOrganization: Organization? = null,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "rejected_at")
    var rejectedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }
}
