package com.astro.leadflow.shared.modules.inventory.api.requests

import com.astro.leadflow.shared.modules.inventory.domain.VehicleCategory
import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class VehicleUpdateRequest(
    val brand: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val color: String? = null,
    val licensePlate: String? = null,
    val salePrice: BigDecimal? = null,
    val costPrice: BigDecimal? = null,
    val mileageKm: Int? = null,
    val ipvaPaid: Boolean? = null,
    val category: VehicleCategory? = null,
    val status: VehicleStatus? = null
)
