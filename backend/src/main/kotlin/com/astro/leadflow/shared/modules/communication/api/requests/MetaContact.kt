package com.astro.leadflow.shared.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaContact(
    val profile: MetaProfile,
    @param:JsonProperty("wa_id") val waId: String
)