package com.astro.leadflow.shared.modules.inventory.api.requests

import com.astro.leadflow.shared.modules.inventory.domain.VehicleStatus
import jakarta.validation.constraints.NotNull

data class UpdateVehicleStatusRequest(
    @field:NotNull val status: VehicleStatus
)