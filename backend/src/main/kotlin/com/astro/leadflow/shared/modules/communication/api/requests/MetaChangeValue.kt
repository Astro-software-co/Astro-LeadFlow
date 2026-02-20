package com.astro.leadflow.shared.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaChangeValue(
    @param:JsonProperty("messaging_product") val messagingProduct: String,
    val metadata: MetaMetadata,
    val contacts: List<MetaContact>? = null,
    val messages: List<MetaMessage>? = null,
    val statuses: List<MetaStatus>? = null
)

