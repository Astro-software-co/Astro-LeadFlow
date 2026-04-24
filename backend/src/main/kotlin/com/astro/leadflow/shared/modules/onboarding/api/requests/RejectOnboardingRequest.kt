package com.astro.leadflow.shared.modules.onboarding.api.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RejectOnboardingRequest(

    @field:NotBlank(message = "Motivo da rejeição é obrigatório")
    @field:Size(max = 1000)
    val rejectionReason: String
)
