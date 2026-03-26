package com.astro.leadflow.shared.modules.ingestion.domain

import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MarketplaceWebhookEventRepository : JpaRepository<MarketplaceWebhookEvent, UUID> {

    fun existsByExternalLeadIdAndMarketplaceType(
        externalLeadId: String,
        marketplaceType: MarketplaceType
    ): Boolean
}
