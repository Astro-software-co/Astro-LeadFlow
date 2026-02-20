package com.astro.leadflow.shared.modules.crm.api.responses

import java.util.UUID

data class SellerSummary(
    val id: UUID,
    val name: String,
    val avatarUrl: String?
)