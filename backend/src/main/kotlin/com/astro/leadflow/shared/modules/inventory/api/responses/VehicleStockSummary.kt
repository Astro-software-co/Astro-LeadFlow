package com.astro.leadflow.shared.modules.inventory.api.responses

data class VehicleStockSummary(
    val available: Long,
    val reserved: Long,
    val maintenance: Long,
    val sold: Long
)