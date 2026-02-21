package com.astro.leadflow.shared.modules.inventory.api.requests

import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class VehicleCreateRequest(
    @field:NotBlank val brand: String,
    @field:NotBlank val model: String,
    @field:NotNull @field:Min(1900) val year: Int,
    val color: String? = null,
    val licensePlate: String? = null,
    @field:NotNull @field:Positive val salePrice: BigDecimal,
    val costPrice: BigDecimal? = null,
    val mileageKm: Int? = null,
    val ipvaPaid: Boolean = false,
    @field:NotNull val category: com.astro.leadflow.modules.inventory.domain.VehicleCategory,
    val status: VehicleStatus = VehicleStatus.AVAILABLE
)

