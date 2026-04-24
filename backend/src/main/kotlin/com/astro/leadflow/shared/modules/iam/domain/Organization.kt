package com.astro.leadflow.shared.modules.iam.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "organizations")
class Organization(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var name: String,

    @Column(nullable = false, unique = true)
    var slug: String,

    @Column(name = "whatsapp_token")
    var whatsappToken: String? = null,

    @Column(name = "whatsapp_phone_number_id")
    var whatsappPhoneNumberId: String? = null,

    @Column(name = "api_key", unique = true)
    var apiKey: String? = null,

    @Column(name = "cnpj", length = 20)
    var cnpj: String? = null,

    @Column(name = "referral_code", length = 32, unique = true)
    var referralCode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrganizationStatus = OrganizationStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }
}

enum class OrganizationStatus { ACTIVE, SUSPENDED, CANCELLED }