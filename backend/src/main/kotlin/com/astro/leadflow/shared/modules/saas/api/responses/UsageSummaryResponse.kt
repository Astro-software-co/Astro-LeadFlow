package com.astro.leadflow.shared.modules.saas.api.responses

import java.time.LocalDate

data class UsageSummaryResponse(
    val planName: String,
    val globalLimit: Long,
    val globalUsed: Long,
    val globalRemaining: Long,
    val renewsAt: LocalDate?,
    val marketplaces: List<MarketplaceUsageDetail>
)