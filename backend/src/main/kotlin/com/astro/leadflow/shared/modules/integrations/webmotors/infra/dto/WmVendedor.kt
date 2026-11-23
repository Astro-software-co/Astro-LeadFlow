package com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WmVendedor(val id: String? = null, val nome: String? = null)
