package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/** Envelope das respostas de domínio/ação: {"Sucesso":true,"Retorno":...,"Mensagens":[...]} */
@JsonIgnoreProperties(ignoreUnknown = true)
data class WmEnvelope<T>(
    @JsonProperty("Sucesso") val sucesso: Boolean? = null,
    @JsonProperty("Retorno") val retorno: T? = null,
    @JsonProperty("Mensagens") val mensagens: List<String>? = null
)
