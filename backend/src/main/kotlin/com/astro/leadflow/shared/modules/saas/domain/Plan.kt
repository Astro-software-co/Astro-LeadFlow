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

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(name = "monthly_price_brl", nullable = false)
    val monthlyPriceBrl: BigDecimal,

    @Column(name = "message_limit", nullable = false)
    val messageLimit: Int,

    @Column(name = "marketplace_limit", nullable = false)
    val marketplaceLimit: Int,

    @Column(name = "billing_cycle", nullable = false)
    var billingCycle: String = "MONTHLY",

    @Column(name = "abacate_product_id")
    var abacateProductId: String? = null,

    @Column(name = "abacate_product_external_id")
    var abacateProductExternalId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
