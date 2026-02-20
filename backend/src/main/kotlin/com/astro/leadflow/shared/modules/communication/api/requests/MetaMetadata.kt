package com.astro.leadflow.shared.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaMetadata(
    @param:JsonProperty("display_phone_number") val displayPhoneNumber: String,
    @param:JsonProperty("phone_number_id") val phoneNumberId: String
)

