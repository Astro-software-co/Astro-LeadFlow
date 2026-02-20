package com.astro.leadflow.modules.inventory.domain

enum class VehicleStatus(val displayName: String) {
    AVAILABLE("Disponível"),
    RESERVED("Reservado"),
    SOLD("Vendido"),
    MAINTENANCE("Em Manutenção")
}

enum class VehicleCategory(val displayName: String) {
    SUV("SUV"),
    SEDAN("Sedan"),
    HATCH("Hatch"),
    PICKUP("Pickup"),
    OTHERS("Outros")
}