package com.astro.leadflow.shared.modules.ingestion.domain

import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import java.time.Instant

data class RawMarketplaceLead(
    val externalId: String,
    val marketplaceType: MarketplaceType,
    val buyerName: String?,
    val buyerEmail: String?,
    val buyerPhone: String?,
    val vehicleBrand: String?,
    val vehicleModel: String?,
    val vehicleYear: Int?,
    val listingId: String?,
    val listingUrl: String?,
    val contactType: String?,
    val receivedAt: Instant,
    /** JSON original do marketplace para auditoria */
    val rawPayload: String
)
