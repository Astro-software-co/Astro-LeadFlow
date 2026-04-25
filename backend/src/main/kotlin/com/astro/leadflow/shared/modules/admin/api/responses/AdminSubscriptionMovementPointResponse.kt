package com.astro.leadflow.shared.modules.admin.api.responses

data class AdminSubscriptionMovementPointResponse(
    val month: String,
    val newSubscriptions: Long,
    val cancellations: Long
)
