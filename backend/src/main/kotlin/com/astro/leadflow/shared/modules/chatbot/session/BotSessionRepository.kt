package com.astro.leadflow.shared.modules.chatbot.session

import com.astro.leadflow.shared.modules.chatbot.domain.entity.BotSession
import com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface BotSessionRepository : JpaRepository<BotSession, UUID> {
    fun findFirstByLeadIdAndStatusOrderByUpdatedAtDesc(
        leadId: UUID,
        status: BotSessionStatus
    ): Optional<BotSession>

    fun findFirstByLeadIdOrderByUpdatedAtDesc(leadId: UUID): Optional<BotSession>

    fun findAllByOrganizationIdAndStatus(
        organizationId: UUID,
        status: BotSessionStatus,
        pageable: Pageable
    ): Page<BotSession>

    fun findAllByOrganizationId(
        organizationId: UUID,
        pageable: Pageable
    ): Page<BotSession>

    fun findByIdAndOrganizationId(id: UUID, organizationId: UUID): Optional<BotSession>

    @Query(
        """
        SELECT COUNT(s)
        FROM BotSession s
        WHERE s.organization.id = :organizationId
        """
    )
    fun countByOrganization(@Param("organizationId") organizationId: UUID): Long

    @Query(
        """
        SELECT COUNT(s)
        FROM BotSession s
        WHERE s.organization.id = :organizationId
        AND s.fallbackCount > 0
        """
    )
    fun countWithFallbackByOrganization(@Param("organizationId") organizationId: UUID): Long

    @Query(
        """
        SELECT COUNT(s)
        FROM BotSession s
        WHERE s.organization.id = :organizationId
        AND s.status = com.astro.leadflow.shared.modules.chatbot.domain.enums.BotSessionStatus.HANDOFF
        """
    )
    fun countHandoffByOrganization(@Param("organizationId") organizationId: UUID): Long

    @Query(
        """
        SELECT s.currentIntent, COUNT(s)
        FROM BotSession s
        WHERE s.organization.id = :organizationId
        AND s.currentIntent IS NOT NULL
        GROUP BY s.currentIntent
        ORDER BY COUNT(s) DESC
        """
    )
    fun topIntentsByOrganization(@Param("organizationId") organizationId: UUID): List<Array<Any>>
}
