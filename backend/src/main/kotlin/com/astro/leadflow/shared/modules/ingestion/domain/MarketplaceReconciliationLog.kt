package com.astro.leadflow.shared.modules.ingestion.domain

import com.astro.leadflow.shared.modules.saas.domain.MarketplaceCredential
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "marketplace_reconciliation_logs")
class MarketplaceReconciliationLog(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", nullable = false)
    val credential: MarketplaceCredential,

    @Column(name = "started_at", nullable = false, updatable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Column(name = "leads_found")
    var leadsFound: Int = 0,

    @Column(name = "leads_new")
    var leadsNew: Int = 0,

    @Column(name = "leads_already_seen")
    var leadsAlreadySeen: Int = 0,

    @Column(name = "status", length = 20)
    var status: String? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null
)
