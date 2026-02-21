package com.astro.leadflow.shared.modules.inventory.api.responses

import java.util.UUID

data class VehiclePhotoResponse(
    val id: UUID,
    val s3Url: String,
    val position: Int
)