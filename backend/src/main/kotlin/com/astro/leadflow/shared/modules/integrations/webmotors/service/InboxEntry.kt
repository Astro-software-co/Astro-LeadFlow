package com.astro.leadflow.shared.modules.integrations.webmotors.service

import com.astro.leadflow.shared.modules.integrations.webmotors.domain.InboxStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** Cópia imutável de uma linha da caixa de entrada (segura de usar fora de transação). */
data class InboxEntry(
    val id: UUID,
    val organizationId: UUID,
    val accountId: UUID,
    val externalId: String,
    val leadType: Int,
    val status: InboxStatus,
    val message: String?,
    val channel: String?,
    val typeDescription: String?,
    val remoteStatus: String?,
    val remoteCreatedAt: String?,
    val adId: String?,
    val adBrand: String?,
    val adModel: String?,
    val adVersion: String?,
    val adYear: Int?,
    val adPlate: String?,
    val adPrice: BigDecimal?,
    val refusalCode: Int?,
    val refusalMessage: String?,
    val leadId: UUID?,
    val attempts: Int,
    val lastError: String?,
    val receivedAt: LocalDateTime,
    val decidedAt: LocalDateTime?
)
