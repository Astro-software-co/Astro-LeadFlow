package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

// Estrutura do payload enviado pela Meta via webhook
data class MetaWebhookRequest(
    @JsonProperty("object") val obj: String,
    val entry: List<MetaEntry>
)

data class MetaChange(
    val value: MetaChangeValue,
    val field: String
)
