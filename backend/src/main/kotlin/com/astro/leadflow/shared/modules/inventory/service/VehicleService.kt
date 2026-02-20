package com.astro.leadflow.modules.inventory.service

import com.astro.leadflow.modules.inventory.api.requests.VehicleCreateRequest
import com.astro.leadflow.modules.inventory.api.requests.VehicleUpdateRequest
import com.astro.leadflow.shared.modules.inventory.api.responses.VehicleResponse
import com.astro.leadflow.shared.modules.inventory.api.responses.VehicleStockSummary
import com.astro.leadflow.modules.inventory.domain.*
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VehicleService(
    private val vehicleRepository: VehicleRepository,
    private val organizationRepository: OrganizationRepository
) {

    @Transactional
    fun create(request: VehicleCreateRequest): VehicleResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val organization = organizationRepository.findById(orgId)
            .orElseThrow { ResourceNotFoundException("Organization", orgId) }

        val vehicle = vehicleRepository.save(
            Vehicle(
                organization = organization,
                brand = request.brand,
                model = request.model,
                year = request.year,
                color = request.color,
                licensePlate = request.licensePlate,
                salePrice = request.salePrice,
                costPrice = request.costPrice,
                mileageKm = request.mileageKm,
                ipvaPaid = request.ipvaPaid,
                category = request.category,
                status = request.status
            )
        )

        return VehicleResponse.from(vehicle)
    }

    @Transactional
    fun update(vehicleId: UUID, request: VehicleUpdateRequest): VehicleResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicle = findVehicleInOrg(vehicleId, orgId)

        request.brand?.let { vehicle.brand = it }
        request.model?.let { vehicle.model = it }
        request.year?.let { vehicle.year = it }
        request.color?.let { vehicle.color = it }
        request.licensePlate?.let { vehicle.licensePlate = it }
        request.salePrice?.let { vehicle.salePrice = it }
        request.costPrice?.let { vehicle.costPrice = it }
        request.mileageKm?.let { vehicle.mileageKm = it }
        request.ipvaPaid?.let { vehicle.ipvaPaid = it }
        request.category?.let { vehicle.category = it }
        request.status?.let { vehicle.status = it }

        return VehicleResponse.from(vehicleRepository.save(vehicle))
    }

    @Transactional(readOnly = true)
    fun findById(vehicleId: UUID): VehicleResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        return VehicleResponse.from(findVehicleInOrg(vehicleId, orgId))
    }

    @Transactional(readOnly = true)
    fun findAll(search: String? = null, status: VehicleStatus? = null): List<VehicleResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicles = when {
            !search.isNullOrBlank() ->
                vehicleRepository.searchByBrandOrModel(orgId, search)
            status != null ->
                vehicleRepository.findAllByOrganizationIdAndStatus(orgId, status)
            else ->
                vehicleRepository.findAllByOrganizationId(orgId)
        }
        return vehicles.map { VehicleResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getStockSummary(): VehicleStockSummary {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val counts = vehicleRepository.countByStatus(orgId)
            .associate { VehicleStatus.valueOf(it[0].toString()) to it[1].toString().toLong() }

        return VehicleStockSummary(
            available = counts[VehicleStatus.AVAILABLE] ?: 0,
            reserved = counts[VehicleStatus.RESERVED] ?: 0,
            maintenance = counts[VehicleStatus.MAINTENANCE] ?: 0,
            sold = counts[VehicleStatus.SOLD] ?: 0
        )
    }

    @Transactional
    fun delete(vehicleId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicle = findVehicleInOrg(vehicleId, orgId)
        vehicleRepository.delete(vehicle)
    }

    // Helper interno — garante multi-tenancy
    fun findVehicleInOrg(vehicleId: UUID, orgId: UUID): Vehicle =
        vehicleRepository.findByOrganizationIdAndId(orgId, vehicleId)
            .orElseThrow { ResourceNotFoundException("Vehicle", vehicleId) }
}