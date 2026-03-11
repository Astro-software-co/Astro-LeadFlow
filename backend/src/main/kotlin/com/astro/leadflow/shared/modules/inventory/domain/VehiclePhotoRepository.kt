package com.astro.leadflow.shared.modules.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VehiclePhotoRepository : JpaRepository<VehiclePhoto, UUID> {
    fun findAllByVehicleIdOrderByPositionAsc(vehicleId: UUID): List<VehiclePhoto>
    fun deleteAllByVehicleId(vehicleId: UUID)
}