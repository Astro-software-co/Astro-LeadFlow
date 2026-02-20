package com.astro.leadflow.shared.modules.crm.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LeadStageHistoryRepository : JpaRepository<LeadStageHistory, UUID> {
    fun findAllByLeadIdOrderByMovedAtDesc(leadId: UUID): List<LeadStageHistory>
}
