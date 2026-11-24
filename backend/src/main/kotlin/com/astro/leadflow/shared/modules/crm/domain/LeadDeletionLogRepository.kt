package com.astro.leadflow.shared.modules.crm.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LeadDeletionLogRepository : JpaRepository<LeadDeletionLog, UUID> {
    fun findAllByOrganizationId(organizationId: UUID): List<LeadDeletionLog>
}
