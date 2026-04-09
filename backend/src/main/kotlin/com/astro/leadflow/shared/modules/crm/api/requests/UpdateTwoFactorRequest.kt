package com.astro.leadflow.shared.modules.iam.api.requests

data class UpdateTwoFactorRequest(
    val emailEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null
)