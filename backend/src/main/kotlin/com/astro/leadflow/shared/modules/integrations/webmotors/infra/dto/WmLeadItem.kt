package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmLeadItem(
    val id: String? = null,
    val dataMensagem: String? = null,
    val dataAceite: String? = null,
    val aceitoPor: String? = null,
    val dataAgendamento: String? = null,
    val status: String? = null,
    val statusLead: Int? = null,
    val codigoMotivoFinalizacao: Int? = null,
    val midia: String? = null,
    val tipoLead: Int? = null,
    val subTipoLead: Int? = null,
    val tipoLeadDesc: String? = null,
    val mensagem: String? = null,
    val loja: WmLoja? = null,
    val anuncio: WmAnuncio? = null,
    val vendedor: WmVendedor? = null
)
