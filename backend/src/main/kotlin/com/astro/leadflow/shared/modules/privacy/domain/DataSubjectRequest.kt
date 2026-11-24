package com.astro.leadflow.shared.modules.privacy.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/** Solicitação de titular (LGPD art. 18). Guarda só o HMAC e uma dica mascarada do identificador. */
@Entity
@Table(name = "data_subject_requests")
class DataSubjectRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: DataSubjectRequestType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: DataSubjectRequestStatus = DataSubjectRequestStatus.RECEIVED,

    @Column(name = "subject_hash", nullable = false, length = 64)
    val subjectHash: String,

    @Column(name = "subject_hint", length = 40)
    val subjectHint: String?,

    @Column(length = 30)
    val channel: String?,

    @Column(name = "received_at", nullable = false)
    val receivedAt: LocalDateTime,

    @Column(name = "due_at", nullable = false)
    val dueAt: LocalDateTime,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @Column(name = "handled_by")
    var handledBy: UUID? = null,

    @Column(name = "result_summary", columnDefinition = "TEXT")
    var resultSummary: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isOpen() = status == DataSubjectRequestStatus.RECEIVED || status == DataSubjectRequestStatus.IN_PROGRESS
}
