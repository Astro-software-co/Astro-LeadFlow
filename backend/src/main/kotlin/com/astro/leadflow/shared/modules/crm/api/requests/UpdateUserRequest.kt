package com.astro.leadflow.shared.modules.iam.api.requests

data class UpdateUserRequest(
    val name: String? = null,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null
)