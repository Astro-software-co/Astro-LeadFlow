package com.astro.leadflow.shared.modules.communication.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {

    fun findAllByLeadIdOrderBySentAtAsc(leadId: UUID): List<ChatMessage>

    fun findByWaMessageId(waMessageId: String): ChatMessage?

    @Query("""
        SELECT COUNT(*) FROM chat_messages
        WHERE organization_id = :orgId
        AND direction = 'OUTBOUND'
        AND EXTRACT(MONTH FROM sent_at) = :month
        AND EXTRACT(YEAR FROM sent_at) = :year
    """, nativeQuery = true)
    fun countOutboundByOrganizationAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Long

    @Query("""
        SELECT AVG(EXTRACT(EPOCH FROM (o.sent_at - i.sent_at)) / 3600)
        FROM chat_messages i
        JOIN chat_messages o ON o.lead_id = i.lead_id
        WHERE i.organization_id = :orgId
        AND i.direction = 'INBOUND'
        AND o.direction = 'OUTBOUND'
        AND o.sent_at > i.sent_at
    """, nativeQuery = true)
    fun avgResponseTimeHoursByOrganization(@Param("orgId") orgId: UUID): Double?
}