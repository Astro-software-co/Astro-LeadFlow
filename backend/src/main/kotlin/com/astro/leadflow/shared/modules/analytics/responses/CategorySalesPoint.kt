package com.astro.leadflow.shared.modules.analytics.api.responses

data class CategorySalesPoint(
    val category: String,
    val sold: Long,
    val inStock: Long
)