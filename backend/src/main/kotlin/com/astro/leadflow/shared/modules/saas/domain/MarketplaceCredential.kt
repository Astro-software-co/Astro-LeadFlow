package com.astro.leadflow.shared.modules.saas.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "marketplace_credentials",
    uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "marketplace_type"])]
)
class MarketplaceCredential(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false, length = 30)
    val marketplaceType: MarketplaceType,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "last_reconciled_at")
    var lastReconciledAt: LocalDateTime? = null,

    // Webmotors
    @Column(name = "wm_client_id")
    var wmClientId: String? = null,

    @Column(name = "wm_client_secret")
    var wmClientSecret: String? = null,

    @Column(name = "wm_access_token")
    var wmAccessToken: String? = null,

    @Column(name = "wm_token_expires_at")
    var wmTokenExpiresAt: LocalDateTime? = null,

    @Column(name = "wm_integrator_email")
    var wmIntegratorEmail: String? = null,

    @Column(name = "wm_cnpj", length = 20)
    var wmCnpj: String? = null,

    // Mercado Livre
    @Column(name = "ml_app_id")
    var mlAppId: String? = null,

    @Column(name = "ml_client_secret")
    var mlClientSecret: String? = null,

    @Column(name = "ml_access_token")
    var mlAccessToken: String? = null,

    @Column(name = "ml_refresh_token")
    var mlRefreshToken: String? = null,

    @Column(name = "ml_token_expires_at")
    var mlTokenExpiresAt: LocalDateTime? = null,

    @Column(name = "ml_user_id")
    var mlUserId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }
}
