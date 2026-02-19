package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaEntry(
    val id: String,
    val changes: List<MetaChange>
)
