package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmKeyValue(
    @JsonProperty("Key") val key: Int? = null,
    @JsonProperty("Value") val value: String? = null
)
