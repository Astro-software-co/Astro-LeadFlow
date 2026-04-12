package com.astro.leadflow.shared.modules.analytics.api.responses

data class RecentSalePoint(
    val clientName: String,
    val vehicleDescription: String,
    val salePrice: Double,
    val soldAt: String,
    val sellerName: String
)
