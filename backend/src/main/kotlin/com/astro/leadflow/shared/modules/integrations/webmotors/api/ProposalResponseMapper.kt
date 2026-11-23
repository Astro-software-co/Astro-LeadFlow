package com.astro.leadflow.shared.modules.integrations.webmotors.api

import com.astro.leadflow.shared.modules.integrations.webmotors.api.responses.ProposalResponse
import com.astro.leadflow.shared.modules.integrations.webmotors.service.InboxEntry

object ProposalResponseMapper {
    fun toResponse(entry: InboxEntry) = with(entry) {
        ProposalResponse(
            id = id, externalId = externalId, leadType = leadType, status = status, message = message,
            channel = channel, typeDescription = typeDescription,
            vehicle = listOfNotNull(adBrand, adModel, adVersion, adYear?.toString()).joinToString(" ").ifBlank { null },
            adId = adId, adPlate = adPlate, adPrice = adPrice, leadId = leadId, refusalCode = refusalCode,
            attempts = attempts, lastError = lastError, receivedAt = receivedAt, decidedAt = decidedAt
        )
    }
}
