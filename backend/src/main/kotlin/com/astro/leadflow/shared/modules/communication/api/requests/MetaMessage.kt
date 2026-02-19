package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaMessage(
    val from: String,
    val id: String,
    val timestamp: String,
    val type: String,
    val text: MetaTextContent? = null
)
