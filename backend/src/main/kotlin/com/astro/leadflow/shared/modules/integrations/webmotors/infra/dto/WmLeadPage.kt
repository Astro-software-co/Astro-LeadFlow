package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmLeadPage(
    val paginaAtual: Int? = null,
    val totalItens: Int? = null,
    val totalPaginas: Int? = null,
    val mensagens: List<WmLeadItem> = emptyList()
)
