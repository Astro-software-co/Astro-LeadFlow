package com.astro.leadflow.shared.modules.inventory.domain

enum class VehicleStatus(val displayName: String) {
    AVAILABLE("Disponível"),
    RESERVED("Reservado"),
    SOLD("Vendido"),
    MAINTENANCE("Em Manutenção")
}