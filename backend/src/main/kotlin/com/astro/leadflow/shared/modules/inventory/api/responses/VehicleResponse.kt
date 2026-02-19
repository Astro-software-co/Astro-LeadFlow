package com.astro.leadflow.modules.inventory.api.responses

import com.astro.leadflow.modules.inventory.domain.Vehicle
import com.astro.leadflow.modules.inventory.domain.VehicleCategory
import com.astro.leadflow.modules.inventory.domain.VehicleStatus
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(vehicle: Vehicle) = VehicleResponse(
            id = vehicle.id,
            brand = vehicle.brand,
            model = vehicle.model,
            year = vehicle.year,
            color = vehicle.color,
            licensePlate = vehicle.licensePlate,
            salePrice = vehicle.salePrice,
            costPrice = vehicle.costPrice,
            mileageKm = vehicle.mileageKm,
            ipvaPaid = vehicle.ipvaPaid,
            category = vehicle.category,
            status = vehicle.status,
            photos = vehicle.photos.map { it.s3Url },
            createdAt = vehicle.createdAt,
            updatedAt = vehicle.updatedAt
        )
    }
}

data class VehicleStockSummary(
    val available: Long,
    val reserved: Long,
    val maintenance: Long,
    val sold: Long
)