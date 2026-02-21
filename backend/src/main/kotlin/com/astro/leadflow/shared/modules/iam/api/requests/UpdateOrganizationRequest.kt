package com.astro.leadflow.shared.modules.iam.api.requests

import jakarta.validation.constraints.NotBlank

data class UpdateOrganizationRequest(
    val name: String? = null,
    val slug: String? = null
)

