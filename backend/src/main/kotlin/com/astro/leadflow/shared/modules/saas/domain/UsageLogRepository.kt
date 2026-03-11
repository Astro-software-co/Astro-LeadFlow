package com.astro.leadflow.shared.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface UsageLogRepository : JpaRepository<UsageLog, UUID> {

    fun findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
        organizationId: UUID,
        marketplaceName: String?,
        month: Int,
        year: Int
    ): Optional<UsageLog>

    @Query("""
        SELECT COALESCE(SUM(u.messagesSent), 0)
        FROM UsageLog u
        WHERE u.organization.id = :orgId
        AND u.periodMonth = :month
        AND u.periodYear = :year
    """)
    fun sumMessagesByOrganizationAndPeriod(
        @Param("orgId") orgId: UUID,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Long

    @Modifying
    @Query("""
        UPDATE UsageLog u
        SET u.messagesSent = u.messagesSent + 1
        WHERE u.organization.id = :orgId
        AND u.marketplaceName = :marketplace
        AND u.periodMonth = :month
        AND u.periodYear = :year
    """)
    fun incrementCount(
        @Param("orgId") orgId: UUID,
        @Param("marketplace") marketplace: String?,
        @Param("month") month: Int,
        @Param("year") year: Int
    ): Int
}