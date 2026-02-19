package com.astro.leadflow.modules.saas.domain

import com.astro.leadflow.modules.iam.domain.Organization
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "marketplace_configs",
    uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "marketplace_name"])]
)
class MarketplaceConfig(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    // Ex: WEBMOTORS, OLX, MERCADO_LIVRE
    @Column(name = "marketplace_name", nullable = false)
    val marketplaceName: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    // Limite de disparos por mês para este marketplace
    @Column(name = "monthly_limit", nullable = false)
    var monthlyLimit: Int = 200,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)