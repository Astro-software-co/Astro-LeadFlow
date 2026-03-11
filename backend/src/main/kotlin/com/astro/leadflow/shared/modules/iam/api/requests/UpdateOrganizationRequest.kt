package com.astro.leadflow.shared.modules.iam.api.requests

data class UpdateOrganizationRequest(
    val name: String? = null,
    val slug: String? = null
)