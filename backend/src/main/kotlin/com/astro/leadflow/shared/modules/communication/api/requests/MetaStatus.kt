package com.astro.leadflow.shared.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaStatus(
    val id: String,
    val status: String,      // sent, delivered, read, failed
    @param:JsonProperty("recipient_id") val recipientId: String
)