package com.astro.leadflow.shared.modules.crm.intake

import java.math.BigDecimal

/** Veículo do anúncio como o marketplace descreveu no momento do contato. */
data class VehicleSnapshot(
    val brand: String? = null,
    val model: String? = null,
    val version: String? = null,
    val year: Int? = null,
    val price: BigDecimal? = null,
    val plate: String? = null
) {
    fun describe(): String? =
        listOfNotNull(brand, model, version, year?.toString()).joinToString(" ").trim().ifBlank { null }
}
