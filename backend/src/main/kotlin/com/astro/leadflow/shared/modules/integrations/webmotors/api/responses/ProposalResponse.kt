package com.astro.leadflow.shared.modules.integrations.webmotors.api.responses

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ProposalResponse(
    val id: UUID,
    val externalId: String,
    val leadType: Int,
    val status: InboxStatus,
    val message: String?,
    val channel: String?,
    val typeDescription: String?,
    val vehicle: String?,
    val adId: String?,
    val adPlate: String?,
    val adPrice: BigDecimal?,
    val leadId: UUID?,
    val refusalCode: Int?,
    val attempts: Int,
    val lastError: String?,
    val receivedAt: LocalDateTime,
    val decidedAt: LocalDateTime?
)
