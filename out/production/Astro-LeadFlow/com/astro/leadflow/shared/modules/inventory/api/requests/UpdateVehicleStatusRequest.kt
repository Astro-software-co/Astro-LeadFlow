package com.astro.leadflow.modules.inventory.api.requests

import com.astro.leadflow.modules.inventory.domain.VehicleStatus
import jakarta.validation.constraints.NotNull

data class UpdateVehicleStatusRequest(
    @field:NotNull val status: VehicleStatus
)