package com.astro.leadflow.modules.analytics.responses

data class KpiResponse(
    val salesThisMonth: KpiCard,
    val totalRevenue: KpiCard,
    val vehiclesInStock: KpiCard,
    val averageTicket: KpiCard,
    val leadsConverted: KpiCard,
    val avgResponseTime: KpiCard,
    val totalLeads: KpiCard
)

data class KpiCard(
    val value: Double,
    val previousValue: Double,
    val unit: KpiUnit,
    val percentageChange: Double = calculateChange(value, previousValue)
) {
    companion object {
        fun calculateChange(current: Double, previous: Double): Double {
            if (previous == 0.0) return 0.0
            return ((current - previous) / previous) * 100
        }
    }
}

enum class KpiUnit { NUMBER, CURRENCY, PERCENTAGE, HOURS }

data class MonthlySalesPoint(
    val month: String,
    val sales: Long,
    val revenue: Double
)

data class CategorySalesPoint(
    val category: String,
    val sold: Long,
    val inStock: Long
)