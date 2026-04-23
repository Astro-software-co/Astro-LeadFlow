package com.astro.leadflow.shared.modules.saas.api.responses

import java.util.UUID

data class SubscriptionCheckoutResponse(
    val planId: UUID,
    val planName: String,
    val checkoutId: String,
    val checkoutUrl: String,
)
