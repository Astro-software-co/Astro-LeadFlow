package com.astro.leadflow.shared.modules.crm.domain

import com.astro.leadflow.modules.iam.domain.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "lead_stage_history")
class LeadStageHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    val lead: Lead,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_by", nullable = true)
    val movedBy: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage")
    val fromStage: PipelineStage? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false)
    val toStage: PipelineStage,

    @Column(name = "moved_at", nullable = false, updatable = false)
    val movedAt: LocalDateTime = LocalDateTime.now()
)