package com.astro.leadflow.shared.modules.ingestion.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "marketplace_webhook_events")
class MarketplaceWebhookEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false, length = 30)
    val marketplaceType: MarketplaceType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    var organization: Organization? = null,

    @Column(name = "external_lead_id")
    val externalLeadId: String? = null,

    @Column(name = "raw_payload", nullable = false, columnDefinition = "JSONB")
    val rawPayload: String,

    @Column(name = "received_at", nullable = false, updatable = false)
    val receivedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "status", nullable = false, length = 20)
    var status: String = WebhookEventStatus.RECEIVED,

    @Column(name = "sqs_message_id")
    var sqsMessageId: String? = null,

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "lead_id")
    var leadId: UUID? = null
)
