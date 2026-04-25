package com.astro.leadflow.shared.modules.referrals.api.responses

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ReferredCompanyResponse(
    val organizationId: UUID,
    val companyName: String,
    val status: String,
    val planCode: String?,
    val commissionBrl: BigDecimal?,
    val approvedAt: LocalDateTime?
)
