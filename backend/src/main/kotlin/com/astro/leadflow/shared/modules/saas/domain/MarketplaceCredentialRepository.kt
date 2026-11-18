package com.astro.leadflow.shared.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MarketplaceCredentialRepository : JpaRepository<MarketplaceCredential, UUID> {

    fun findByOrganizationIdAndMarketplaceTypeAndIsActiveTrue(
        organizationId: UUID,
        marketplaceType: MarketplaceType
    ): MarketplaceCredential?

    fun findAllByMarketplaceTypeAndIsActiveTrue(
        marketplaceType: MarketplaceType
    ): List<MarketplaceCredential>

    fun findAllByIsActiveTrue(): List<MarketplaceCredential>

    @Query("SELECT c FROM MarketplaceCredential c WHERE c.organization.id = :orgId AND c.isActive = true")
    fun findActiveByOrganization(@Param("orgId") orgId: UUID): List<MarketplaceCredential>

    fun findByMlUserIdAndMarketplaceTypeAndIsActiveTrue(
        mlUserId: Long,
        marketplaceType: MarketplaceType
    ): MarketplaceCredential?
}
