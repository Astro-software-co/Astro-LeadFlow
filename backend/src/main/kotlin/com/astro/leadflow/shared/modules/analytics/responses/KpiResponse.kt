package com.astro.leadflow.shared.modules.analytics.api.responses

data class KpiResponse(
    val salesThisMonth: KpiCard,
    val totalRevenue: KpiCard,
    val vehiclesInStock: KpiCard,
    val averageTicket: KpiCard,
    val leadsConverted: KpiCard,
    val avgResponseTime: KpiCard,
    val totalLeads: KpiCard
)