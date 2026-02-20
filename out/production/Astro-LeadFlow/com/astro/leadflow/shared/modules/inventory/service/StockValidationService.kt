package com.astro.leadflow.modules.inventory.service

import com.astro.leadflow.modules.inventory.domain.VehicleRepository
import com.astro.leadflow.modules.inventory.domain.VehicleStatus
import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StockValidationService(
    private val vehicleRepository: VehicleRepository
) {
    /**
     * Valida se um veículo pode ser reservado ou vendido.
     * Chamado por outros módulos antes de vincular veículo a um lead/deal.
     */
    fun assertVehicleAvailable(vehicleId: UUID, organizationId: UUID) {
        val vehicle = vehicleRepository.findByOrganizationIdAndId(organizationId, vehicleId)
            .orElseThrow { ResourceNotFoundException("Vehicle", vehicleId) }

        if (!vehicle.isAvailable()) {
            throw BusinessException(
                "Veículo '${vehicle.brand} ${vehicle.model}' não está disponível. " +
                "Status atual: ${vehicle.status.displayName}."
            )
        }
    }

    fun markAsReserved(vehicleId: UUID, organizationId: UUID) {
        val vehicle = vehicleRepository.findByOrganizationIdAndId(organizationId, vehicleId)
            .orElseThrow { ResourceNotFoundException("Vehicle", vehicleId) }
        vehicle.status = VehicleStatus.RESERVED
        vehicleRepository.save(vehicle)
    }

    fun markAsSold(vehicleId: UUID, organizationId: UUID) {
        val vehicle = vehicleRepository.findByOrganizationIdAndId(organizationId, vehicleId)
            .orElseThrow { ResourceNotFoundException("Vehicle", vehicleId) }
        vehicle.status = VehicleStatus.SOLD
        vehicleRepository.save(vehicle)
    }
}