package com.astro.leadflow.shared.modules.iam.api.requests

import jakarta.validation.constraints.NotBlank

data class UpdateWhatsappCredentialsRequest(
    @field:NotBlank val whatsappToken: String,
    @field:NotBlank val whatsappPhoneNumberId: String
)