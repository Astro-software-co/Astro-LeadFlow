package com.astro.leadflow.shared.modules.inventory.service

import com.astro.leadflow.shared.modules.inventory.api.requests.VehicleCreateRequest
import com.astro.leadflow.shared.modules.inventory.api.requests.VehicleUpdateRequest
import com.astro.leadflow.shared.modules.inventory.domain.VehicleRepository
import com.astro.leadflow.shared.modules.crm.service.VehicleStatus
import com.astro.leadflow.shared.modules.inventory.mapper.VehicleMapper
import com.astro.leadflow.modules.iam.domain.Organization
import com.astro.leadflow.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.modules.inventory.domain.*
import com.astro.leadflow.shared.exception.InvalidOperationException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class VehicleServiceTest {

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var vehicleMapper: VehicleMapper
    private lateinit var vehicleService: VehicleService

    private val orgId = UUID.randomUUID()
    private val organization = Organization(id = orgId, name = "Test", slug = "test")

    @BeforeEach
    fun setup() {
        vehicleRepository = mockk()
        organizationRepository = mockk()
        vehicleMapper = mockk()
        vehicleService = VehicleService(vehicleRepository, organizationRepository, vehicleMapper)

        mockkObject(com.astro.leadflow.shared.util.SecurityUtils)
        every {
            com.astro.leadflow.shared.util.SecurityUtils.getCurrentOrganizationId()
        } returns orgId
    }

    @Test
    fun `create should save vehicle and return response`() {
        val request = buildCreateRequest()
        val vehicle = buildVehicle()

        every { organizationRepository.findById(orgId) } returns Optional.of(organization)
        every { vehicleRepository.save(any()) } returns vehicle
        every { vehicleMapper.toResponse(vehicle) } returns mockk()

        vehicleService.create(request)

        verify(exactly = 1) { vehicleRepository.save(any()) }
    }

    @Test
    fun `delete should throw when vehicle is reserved`() {
        val vehicle = buildVehicle(status = VehicleStatus.RESERVED)
        every {
            vehicleRepository.findByOrganizationIdAndId(orgId, vehicle.id)
        } returns Optional.of(vehicle)

        assertThrows<InvalidOperationException> {
            vehicleService.delete(vehicle.id)
        }
    }

    @Test
    fun `delete should throw when vehicle is sold`() {
        val vehicle = buildVehicle(status = VehicleStatus.SOLD)
        every {
            vehicleRepository.findByOrganizationIdAndId(orgId, vehicle.id)
        } returns Optional.of(vehicle)

        assertThrows<InvalidOperationException> {
            vehicleService.delete(vehicle.id)
        }
    }

    @Test
    fun `update should throw when trying to undo a sale`() {
        val vehicle = buildVehicle(status = VehicleStatus.SOLD)
        val request = VehicleUpdateRequest(status = VehicleStatus.AVAILABLE)

        every {
            vehicleRepository.findByOrganizationIdAndId(orgId, vehicle.id)
        } returns Optional.of(vehicle)

        assertThrows<InvalidOperationException> {
            vehicleService.update(vehicle.id, request)
        }
    }

    @Test
    fun `findById should throw when vehicle not found`() {
        val vehicleId = UUID.randomUUID()
        every {
            vehicleRepository.findByOrganizationIdAndId(orgId, vehicleId)
        } returns Optional.empty()

        assertThrows<ResourceNotFoundException> {
            vehicleService.findById(vehicleId)
        }
    }

    private fun buildCreateRequest() = VehicleCreateRequest(
        brand = "Toyota",
        model = "Corolla",
        year = 2022,
        salePrice = BigDecimal("95000"),
        category = VehicleCategory.SEDAN
    )

    private fun buildVehicle(status: VehicleStatus = VehicleStatus.AVAILABLE) = Vehicle(
        id = UUID.randomUUID(),
        organization = organization,
        brand = "Toyota",
        model = "Corolla",
        year = 2022,
        salePrice = BigDecimal("95000"),
        category = VehicleCategory.SEDAN,
        status = status
    )
}