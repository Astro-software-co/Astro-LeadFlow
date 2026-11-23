package com.astro.leadflow.shared.modules.integrations.webmotors.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface WebmotorsLeadInboxRepository : JpaRepository<WebmotorsLeadInbox, UUID> {

    fun findByOrganizationIdAndExternalIdAndLeadType(
        organizationId: UUID,
        externalId: String,
        leadType: Int
    ): Optional<WebmotorsLeadInbox>

    fun findByOrganizationIdAndId(organizationId: UUID, id: UUID): Optional<WebmotorsLeadInbox>

    fun findAllByOrganizationIdOrderByReceivedAtDesc(organizationId: UUID): List<WebmotorsLeadInbox>

    fun findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(
        organizationId: UUID,
        status: InboxStatus
    ): List<WebmotorsLeadInbox>

    fun findAllByAccountIdAndStatusIn(accountId: UUID, statuses: Collection<InboxStatus>): List<WebmotorsLeadInbox>
}
