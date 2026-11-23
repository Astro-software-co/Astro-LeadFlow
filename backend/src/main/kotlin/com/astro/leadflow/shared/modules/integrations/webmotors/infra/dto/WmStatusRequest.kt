package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WmStatusRequest(
    @JsonProperty("tipo_lead") val tipoLead: Int?,
    @JsonProperty("codigo_motivo") val codigoMotivo: Int?,
    val mensagem: String? = null
)
