package com.astro.leadflow.modules.iam.domain

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