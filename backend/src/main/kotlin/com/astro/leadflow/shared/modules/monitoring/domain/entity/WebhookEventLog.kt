package com.astro.leadflow.shared.modules.monitoring.domain.entity

import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookOutcome
import com.astro.leadflow.shared.modules.monitoring.domain.enums.WebhookSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "webhook_event_log")
class WebhookEventLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "received_at", nullable = false)
    val receivedAt: OffsetDateTime = OffsetDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: WebhookSource,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val outcome: WebhookOutcome,

    @Column(name = "http_status", nullable = false)
    val httpStatus: Int,

    @Column(name = "correlation_id")
    val correlationId: String? = null,

    @Column(name = "duration_ms")
    val durationMs: Int? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null
)
