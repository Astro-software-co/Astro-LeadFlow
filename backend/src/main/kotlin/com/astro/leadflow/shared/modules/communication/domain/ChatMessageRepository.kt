package com.astro.leadflow.shared.modules.communication.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {

    fun findAllByLeadIdOrderBySentAtAsc(leadId: UUID): List<ChatMessage>

    fun findByWaMessageId(waMessageId: String): ChatMessage?

    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.organization.id = :orgId
        AND m.direction = 'OUTBOUND'
        AND MONTH(m.sentAt) = :month
        AND YEAR(m.sentAt) = :year
    """)
    fun countOutboundByOrganizationAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Long

    @Query("""
        SELECT AVG(
            FUNCTION('TIMESTAMPDIFF', HOUR, inbound.sentAt, outbound.sentAt)
        )
        FROM ChatMessage inbound
        JOIN ChatMessage outbound ON outbound.lead.id = inbound.lead.id
        WHERE inbound.organization.id = :orgId
        AND inbound.direction = 'INBOUND'
        AND outbound.direction = 'OUTBOUND'
        AND outbound.sentAt > inbound.sentAt
    """)
    fun avgResponseTimeHoursByOrganization(@Param("orgId") orgId: UUID): Double?
}