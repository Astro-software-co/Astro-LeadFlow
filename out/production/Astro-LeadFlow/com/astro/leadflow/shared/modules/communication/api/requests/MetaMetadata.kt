package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaMetadata(
    @JsonProperty("display_phone_number") val displayPhoneNumber: String,
    @JsonProperty("phone_number_id") val phoneNumberId: String
)

