package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

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
