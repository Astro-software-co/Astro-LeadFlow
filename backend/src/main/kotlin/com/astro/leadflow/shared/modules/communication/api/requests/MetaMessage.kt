package com.astro.leadflow.shared.modules.communication.api.requests

data class MetaMessage(
    val from: String,
    val id: String,
    val timestamp: String,
    val type: String,
    val text: MetaTextContent? = null
)
