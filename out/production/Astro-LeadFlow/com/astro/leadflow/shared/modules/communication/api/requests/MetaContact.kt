package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaContact(
    val profile: MetaProfile,
    val wa_id: String
)
