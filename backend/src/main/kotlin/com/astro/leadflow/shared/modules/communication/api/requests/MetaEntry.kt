package com.astro.leadflow.shared.modules.communication.api.requests
import com.astro.leadflow.shared.modules.communication.api.requests.MetaChange

data class MetaEntry(
    val id: String,
    val changes: List<MetaChange>
)
