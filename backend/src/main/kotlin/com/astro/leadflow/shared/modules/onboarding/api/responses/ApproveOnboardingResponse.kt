package com.astro.leadflow.shared.modules.onboarding.api.responses

import java.util.UUID

data class ApproveOnboardingResponse(
    val organizationId: UUID,
    val organizationName: String,
    val ownerEmail: String,
    val referralCode: String
)
