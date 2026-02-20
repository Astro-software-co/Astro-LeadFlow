package com.astro.leadflow.modules.communication.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

// Estrutura do payload enviado pela Meta via webhook
data class MetaWebhookRequest(
    @JsonProperty("object") val obj: String,
    val entry: List<MetaEntry>
)

data class MetaEntry(
    val id: String,
    val changes: List<MetaChange>
)

data class MetaChange(
    val value: MetaChangeValue,
    val field: String
)

data class MetaChangeValue(
    @JsonProperty("messaging_product") val messagingProduct: String,
    val metadata: MetaMetadata,
    val contacts: List<MetaContact>? = null,
    val messages: List<MetaMessage>? = null,
    val statuses: List<MetaStatus>? = null
)

data class MetaMetadata(
    @JsonProperty("display_phone_number") val displayPhoneNumber: String,
    @JsonProperty("phone_number_id") val phoneNumberId: String
)

data class MetaContact(
    val profile: MetaProfile,
    val wa_id: String
)

data class MetaProfile(val name: String)

data class MetaMessage(
    val from: String,
    val id: String,
    val timestamp: String,
    val type: String,
    val text: MetaTextContent? = null
)

data class MetaTextContent(val body: String)

data class MetaStatus(
    val id: String,
    val status: String,      // sent, delivered, read, failed
    @JsonProperty("recipient_id") val recipientId: String
)