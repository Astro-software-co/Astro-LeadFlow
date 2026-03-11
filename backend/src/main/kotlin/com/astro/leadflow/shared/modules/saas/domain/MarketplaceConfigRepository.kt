package com.astro.leadflow.shared.modules.saas.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MarketplaceConfigRepository : JpaRepository<MarketplaceConfig, UUID> {
    fun findAllByOrganizationId(organizationId: UUID): List<MarketplaceConfig>
    fun findByOrganizationIdAndMarketplaceName(
        organizationId: UUID,
        marketplaceName: String
    ): Optional<MarketplaceConfig>
    fun countByOrganizationId(organizationId: UUID): Long
}