package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaStatus(
    val id: String,
    val status: String,      // sent, delivered, read, failed
    @JsonProperty("recipient_id") val recipientId: String
)