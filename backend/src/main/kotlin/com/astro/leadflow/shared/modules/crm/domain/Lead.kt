package com.astro.leadflow.shared.modules.crm.domain

import com.astro.leadflow.modules.iam.domain.Organization
import com.astro.leadflow.modules.iam.domain.User
import com.astro.leadflow.shared.modules.crm.service.Vehicle
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "leads")
class Lead(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = true)
    var assignedTo: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = true)
    var vehicle: Vehicle? = null,

    @Column(name = "name")
    var name: String? = null,

    @Column(name = "phone_number", nullable = false)
    var phoneNumber: String,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "origin", nullable = false)
    var origin: String,

    @Column(name = "vehicle_interest")
    var vehicleInterest: String? = null,

    @Column(name = "budget")
    var budget: BigDecimal? = null,

    @Column(name = "urgency")
    var urgency: String? = null,

    @Column(name = "purchase_forecast")
    var purchaseForecast: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type")
    var interestType: InterestType? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_stage", nullable = false)
    var pipelineStage: PipelineStage = PipelineStage.TALKING_TO_BOT,

    @Column(name = "is_duplicate", nullable = false)
    var isDuplicate: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }

    fun isAssigned(): Boolean = assignedTo != null

    fun belongsToOrganization(organizationId: UUID): Boolean =
        organization.id == organizationId
}

enum class InterestType(val displayName: String) {
    CASH_PURCHASE("Compra à vista"),
    FINANCING("Financiamento"),
    TRADE_IN("Troca com troco")
}