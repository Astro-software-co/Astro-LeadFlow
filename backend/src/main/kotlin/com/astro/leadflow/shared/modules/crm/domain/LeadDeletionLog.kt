package com.astro.leadflow.shared.modules.crm.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/** Auditoria de exclusão manual de lead. Nunca guarda nome, telefone ou e-mail (só o HMAC do telefone). */
@Entity
@Table(name = "lead_deletion_log")
class LeadDeletionLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,

    @Column(name = "deleted_by")
    val deletedBy: UUID?,

    @Column(name = "deleted_at", nullable = false)
    val deletedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "subject_hash", nullable = false, length = 64)
    val subjectHash: String,

    @Column(name = "cards_deleted", nullable = false)
    val cardsDeleted: Int,

    @Column(name = "messages_deleted", nullable = false)
    val messagesDeleted: Int,

    @Column(length = 500)
    val reason: String?
)
