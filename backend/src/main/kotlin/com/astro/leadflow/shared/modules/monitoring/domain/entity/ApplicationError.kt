package com.astro.leadflow.shared.modules.monitoring.domain.entity

import com.astro.leadflow.shared.modules.monitoring.domain.enums.LogLevel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "application_error")
class ApplicationError(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val level: LogLevel,

    @Column(name = "logger_name", nullable = false)
    val loggerName: String,

    @Column(name = "correlation_id")
    val correlationId: String? = null,

    @Column(name = "organization_id")
    val organizationId: UUID? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    val stackTrace: String? = null,

    @Column(name = "request_path")
    val requestPath: String? = null,

    @Column(name = "request_method")
    val requestMethod: String? = null,

    @Column(name = "user_id")
    val userId: UUID? = null
)
