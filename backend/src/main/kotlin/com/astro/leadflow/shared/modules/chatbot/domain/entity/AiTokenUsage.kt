package com.astro.leadflow.shared.modules.chatbot.domain.entity

import com.astro.leadflow.shared.modules.chatbot.domain.enums.EngineType
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
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ai_token_usage")
class AiTokenUsage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_session_id", nullable = false)
    val botSession: BotSession,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val engine: EngineType,

    @Column(name = "tokens_input", nullable = false)
    val tokensInput: Int,

    @Column(name = "tokens_output", nullable = false)
    val tokensOutput: Int,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: OffsetDateTime = OffsetDateTime.now()
)
