package com.astro.leadflow.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface PlanRepository : JpaRepository<Plan, UUID> {
    fun findByName(name: String): Optional<Plan>
}

interface SubscriptionRepository : JpaRepository<Subscription, UUID> {
    fun findByOrganizationId(organizationId: UUID): Optional<Subscription>
}

interface MarketplaceConfigRepository : JpaRepository<MarketplaceConfig, UUID> {

    fun findAllByOrganizationId(organizationId: UUID): List<MarketplaceConfig>

    fun findByOrganizationIdAndMarketplaceName(
        organizationId: UUID,
        marketplaceName: String
    ): Optional<MarketplaceConfig>

    fun countByOrganizationId(organizationId: UUID): Long
}

interface UsageLogRepository : JpaRepository<UsageLog, UUID> {

    fun findByOrganizationIdAndMarketplaceNameAndPeriodMonthAndPeriodYear(
        organizationId: UUID,
        marketplaceName: String?,
        month: Int,
        year: Int
    ): Optional<UsageLog>

    // Total de mensagens enviadas no mês pela organização (todos marketplaces)
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

    // Incremento atômico para evitar race condition
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