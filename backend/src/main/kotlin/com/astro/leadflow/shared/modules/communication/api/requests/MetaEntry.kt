package com.astro.leadflow.shared.modules.communication.api.requests

data class MetaEntry(
    val id: String,
    val changes: List<MetaChange>
)