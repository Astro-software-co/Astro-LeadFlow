package com.astro.leadflow.shared.modules.saas.api.requests

import jakarta.validation.constraints.Pattern

data class CreateSubscriptionCheckoutRequest(
    @field:Pattern(regexp = "^https?://.*", message = "returnUrl deve ser uma URL HTTP(S) válida")
    val returnUrl: String? = null,

    @field:Pattern(regexp = "^https?://.*", message = "completionUrl deve ser uma URL HTTP(S) válida")
    val completionUrl: String? = null,
)
