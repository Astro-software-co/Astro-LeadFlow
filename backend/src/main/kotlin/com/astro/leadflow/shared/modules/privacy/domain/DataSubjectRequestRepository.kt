package com.astro.leadflow.shared.modules.privacy.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface DataSubjectRequestRepository : JpaRepository<DataSubjectRequest, UUID> {
    fun findByOrganizationIdAndId(organizationId: UUID, id: UUID): Optional<DataSubjectRequest>
    fun findAllByOrganizationIdOrderByReceivedAtDesc(organizationId: UUID): List<DataSubjectRequest>
    fun findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(
        organizationId: UUID,
        status: DataSubjectRequestStatus
    ): List<DataSubjectRequest>
}
