package com.astro.leadflow.shared.modules.saas.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "plans")
class Plan(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    @Column(name = "price_brl", nullable = false)
    val priceBrl: BigDecimal,

    @Column(name = "message_limit", nullable = false)
    val messageLimit: Int,

    @Column(name = "marketplace_limit", nullable = false)
    val marketplaceLimit: Int,

    @Column(nullable = false, unique = true)
    val code: String = "plan-${UUID.randomUUID()}",

    @Column(nullable = false)
    val description: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    var billingCycle: BillingCycle = BillingCycle.MONTHLY,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "abacate_product_id")
    var abacateProductId: String? = null,

    @Column(name = "abacate_product_external_id")
    var abacateProductExternalId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_engine_tier", nullable = false)
    var aiEngineTier: AiEngineTier = AiEngineTier.NONE,

    @Column(name = "max_ai_tokens_per_month")
    var maxAiTokensPerMonth: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isSelfServiceAvailable(): Boolean = priceBrl > BigDecimal.ZERO

    fun displayName(): String = "$name ${billingCycle.label}"
}
