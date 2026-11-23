package com.astro.leadflow.shared.modules.chatbot.domain.entity

import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotIntent
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus
import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
import com.astro.leadflow.shared.modules.chatbot.domain.enums.HandoffReason
import com.astro.leadflow.shared.modules.crm.domain.Lead
import com.astro.leadflow.shared.modules.iam.domain.Organization
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "bot_session")
class BotSession(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    val lead: Lead,

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_used", nullable = false)
    var engineUsed: EngineType = EngineType.NONE,

    @Enumerated(EnumType.STRING)
    @Column(name = "current_intent")
    var currentIntent: BotIntent? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "slot_values", nullable = false, columnDefinition = "jsonb")
    var slotValues: MutableMap<String, String> = mutableMapOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BotSessionStatus = BotSessionStatus.ACTIVE,

    @Column(name = "fallback_count", nullable = false)
    var fallbackCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "handoff_reason")
    var handoffReason: HandoffReason? = null,

    @Column(name = "handoff_at")
    var handoffAt: OffsetDateTime? = null,

    @Column(name = "last_message_at")
    var lastMessageAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = OffsetDateTime.now()
    }

    fun markHandoff(reason: HandoffReason) {
        status = BotSessionStatus.HANDOFF
        handoffReason = reason
        handoffAt = OffsetDateTime.now()
    }
}
