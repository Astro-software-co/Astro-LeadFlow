package com.astro.leadflow.shared.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaWebhookRequest(
    @param:JsonProperty("object") val obj: String,
    val entry: List<MetaEntry>
)


