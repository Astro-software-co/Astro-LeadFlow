package com.astro.leadflow.modules.inventory.api.responses

import com.astro.leadflow.modules.inventory.domain.Vehicle
import com.astro.leadflow.modules.inventory.domain.VehicleCategory
import com.astro.leadflow.modules.inventory.domain.VehicleStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class VehicleStockSummary(
    val available: Long,
    val reserved: Long,
    val maintenance: Long,
    val sold: Long
)