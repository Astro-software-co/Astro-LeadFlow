package com.astro.leadflow.shared.modules.ingestion.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class MercadoLivreWebhookRequest(
    @JsonProperty("_id") val notificationId: String,
    @JsonProperty("resource") val resource: String,           // "/vis/leads/{id}"
    @JsonProperty("user_id") val userId: Long,
    @JsonProperty("topic") val topic: String,
    @JsonProperty("application_id") val applicationId: Long,
    @JsonProperty("attempts") val attempts: Int,
    @JsonProperty("sent") val sent: String,
    @JsonProperty("received") val received: String? = null
)
