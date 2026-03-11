package com.astro.leadflow.shared.modules.saas.api.responses

data class MarketplaceUsageDetail(
    val marketplaceName: String,
    val enabled: Boolean,
    val used: Long,
    val limit: Long,
    val remaining: Long
)