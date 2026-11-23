package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/*
 * DTOs da API de Leads da Webmotors, modelados a partir da collection Postman de homologação.
 * Tudo é anulável e com ignoreUnknown: o formato real será confirmado na homologação.
 * IDs vêm como número no exemplo; o Jackson converte para String.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmTokenResponse(
    @JsonProperty("access_token") val accessToken: String? = null,
    @JsonProperty("token_type") val tokenType: String? = null,
    @JsonProperty("expires_in") val expiresIn: Long? = null
)
