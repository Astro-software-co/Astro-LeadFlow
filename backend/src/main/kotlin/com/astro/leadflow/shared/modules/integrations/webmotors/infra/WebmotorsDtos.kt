package com.astro.leadflow.shared.modules.integrations.webmotors.infra

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

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

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmLeadPage(
    val paginaAtual: Int? = null,
    val totalItens: Int? = null,
    val totalPaginas: Int? = null,
    val mensagens: List<WmLeadItem> = emptyList()
)

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

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmLoja(val id: String? = null, val nomeFantasia: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmAnuncio(
    val id: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val versao: String? = null,
    val anoModelo: Int? = null,
    val anoFabricacao: Int? = null,
    val valor: BigDecimal? = null,
    val placa: String? = null,
    val tipoVeiculo: String? = null,
    val blindado: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmVendedor(val id: String? = null, val nome: String? = null)

/** Envelope das respostas de domínio/ação: {"Sucesso":true,"Retorno":...,"Mensagens":[...]} */
@JsonIgnoreProperties(ignoreUnknown = true)
data class WmEnvelope<T>(
    @JsonProperty("Sucesso") val sucesso: Boolean? = null,
    @JsonProperty("Retorno") val retorno: T? = null,
    @JsonProperty("Mensagens") val mensagens: List<String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmKeyValue(
    @JsonProperty("Key") val key: Int? = null,
    @JsonProperty("Value") val value: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WmDecisionRequest(
    val aceito: Boolean,
    @JsonProperty("tipo_lead") val tipoLead: Int?,
    @JsonProperty("codigo_motivo") val codigoMotivo: Int? = null,
    val mensagem: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WmStatusRequest(
    @JsonProperty("tipo_lead") val tipoLead: Int?,
    @JsonProperty("codigo_motivo") val codigoMotivo: Int?,
    val mensagem: String? = null
)
