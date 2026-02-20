package com.astro.leadflow.modules.saas.domain

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

    // Limite total de mensagens por mês para a organização
    @Column(name = "message_limit", nullable = false)
    val messageLimit: Int,

    // Quantos marketplaces a organização pode cadastrar
    @Column(name = "marketplace_limit", nullable = false)
    val marketplaceLimit: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)