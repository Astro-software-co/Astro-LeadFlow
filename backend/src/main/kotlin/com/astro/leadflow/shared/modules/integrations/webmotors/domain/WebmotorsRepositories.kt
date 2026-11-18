package com.astro.leadflow.shared.modules.integrations.webmotors.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface WebmotorsAccountRepository : JpaRepository<WebmotorsAccount, UUID> {

    fun findByOrganizationId(organizationId: UUID): Optional<WebmotorsAccount>

    fun findAllByEnabledTrue(): List<WebmotorsAccount>

    /** Reserva a conta para esta instância. Retorna 1 se conseguiu, 0 se outra instância já a detém. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE WebmotorsAccount a SET a.leaseUntil = :until
        WHERE a.id = :id AND (a.leaseUntil IS NULL OR a.leaseUntil < :now)
        """
    )
    fun claimLease(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
        @Param("until") until: LocalDateTime
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WebmotorsAccount a SET a.leaseUntil = NULL WHERE a.id = :id")
    fun releaseLease(@Param("id") id: UUID): Int
}

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
