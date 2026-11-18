package com.astro.leadflow.shared.modules.ingestion.api.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class WebmotorsWebhookRequest(
    @JsonProperty("IdLead") val idLead: String,
    @JsonProperty("CodigoCliente") val codigoCliente: String,
    @JsonProperty("Cnpj") val cnpj: String,
    @JsonProperty("IdTipoLead") val idTipoLead: Int
)
