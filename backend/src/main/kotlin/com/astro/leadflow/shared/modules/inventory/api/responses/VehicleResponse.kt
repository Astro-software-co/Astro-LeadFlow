package com.astro.leadflow.shared.modules.inventory.api.responses

import com.astro.leadflow.shared.modules.inventory.domain.VehicleCategory
import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class VehicleResponse(
    val id: UUID,
    val brand: String,
    val model: String,
    val year: Int,
    val color: String?,
    val licensePlate: String?,
    val salePrice: BigDecimal,
    val costPrice: BigDecimal?,
    val mileageKm: Int?,
    val ipvaPaid: Boolean,
    val category: VehicleCategory,
    val status: VehicleStatus,
    val photos: List<String>,
    val organizationId: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
