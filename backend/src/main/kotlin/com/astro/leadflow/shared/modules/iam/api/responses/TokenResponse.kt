package com.astro.leadflow.modules.iam.api.responses

import java.util.UUID

data class TokenResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: UUID? = null,
    val role: String? = null,
    val organizationId: UUID? = null,
    val pendingTwoFactor: Boolean = false
)