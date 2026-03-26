package com.astro.leadflow.shared.modules.ingestion.domain

import com.astro.leadflow.shared.modules.saas.domain.MarketplaceType
import java.time.Instant
import java.util.UUID

data class MarketplaceLeadEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val marketplaceType: MarketplaceType,
    /** Preenchido quando a org é conhecida no momento do webhook (ex: Webmotors via CNPJ) */
    val organizationId: UUID? = null,
    val externalLeadId: String,
    /** ML user_id para resolver a org no consumer */
    val externalUserId: String? = null,
    /** CNPJ enviado pelo Webmotors para resolver a org no consumer */
    val cnpj: String? = null,
    /** Webmotors IdTipoLead */
    val leadTypeId: Int? = null,
    /** ML contact_type extraído do resource */
    val contactType: String? = null,
    val webhookEventId: UUID,
    val receivedAt: Instant = Instant.now()
)
