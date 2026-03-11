package com.astro.leadflow.shared.modules.inventory.mapper

import com.astro.leadflow.shared.modules.inventory.api.responses.VehicleResponse
import com.astro.leadflow.shared.modules.inventory.api.responses.VehicleStockSummary
import com.astro.leadflow.shared.modules.inventory.domain.Vehicle
import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import org.springframework.stereotype.Component

@Component
class VehicleMapper {

    fun toResponse(vehicle: Vehicle): VehicleResponse = VehicleResponse(
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
        photos = vehicle.photos
            .sortedBy { it.position }
            .map { it.s3Url },
        organizationId = vehicle.organization.id,
        createdAt = vehicle.createdAt,
        updatedAt = vehicle.updatedAt
    )

    fun toStockSummary(counts: Map<VehicleStatus, Long>): VehicleStockSummary =
        VehicleStockSummary(
            available = counts[VehicleStatus.AVAILABLE] ?: 0L,
            reserved = counts[VehicleStatus.RESERVED] ?: 0L,
            maintenance = counts[VehicleStatus.MAINTENANCE] ?: 0L,
            sold = counts[VehicleStatus.SOLD] ?: 0L
        )
}