package com.astro.leadflow.shared.modules.chatbot.usage

import com.astro.leadflow.shared.modules.chatbot.domain.entity.AiTokenUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface AiTokenUsageRepository : JpaRepository<AiTokenUsage, UUID> {
    @Query(
        """
        SELECT COALESCE(SUM(u.tokensInput + u.tokensOutput), 0)
        FROM AiTokenUsage u
        WHERE u.organization.id = :organizationId
        AND u.occurredAt >= :from
        AND u.occurredAt < :to
        """
    )
    fun sumTokensByOrganizationAndPeriod(
        @Param("organizationId") organizationId: UUID,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime
    ): Long

    @Query(
        """
        SELECT COALESCE(SUM(u.tokensInput + u.tokensOutput), 0)
        FROM AiTokenUsage u
        WHERE u.organization.id = :organizationId
        """
    )
    fun sumTokensByOrganization(@Param("organizationId") organizationId: UUID): Long
}
